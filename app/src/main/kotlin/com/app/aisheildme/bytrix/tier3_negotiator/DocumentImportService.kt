package com.app.aisheildme.bytrix.tier3_negotiator

import android.net.Uri
import com.app.aisheildme.bytrix.tier2_vault.entities.BankStatementEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InsurancePolicyEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.MedicationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PatientEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorDocumentEntity
import com.app.aisheildme.bytrix.tier2_vault.repository.BankingRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.DocumentSaveResult
import com.app.aisheildme.bytrix.tier2_vault.repository.ExtractedField
import com.app.aisheildme.bytrix.tier2_vault.repository.HealthcareRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.NewPurchase
import com.app.aisheildme.bytrix.tier2_vault.repository.RetailRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.VaultDocumentRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.VaultIdentity
import com.app.aisheildme.bytrix.tier2_vault.repository.VaultValidationException
import com.app.aisheildme.bytrix.tier2_vault.security.AuditLogger
import com.app.aisheildme.bytrix.tier3_negotiator.ai.GeminiNanoManager
import com.app.aisheildme.bytrix.tier4_vendor.ocr.DocumentClassifier
import com.app.aisheildme.bytrix.tier4_vendor.ocr.DocumentType
import com.app.aisheildme.bytrix.tier4_vendor.ocr.FormFieldExtractor
import com.app.aisheildme.bytrix.tier4_vendor.ocr.FormMapper
import com.app.aisheildme.bytrix.tier4_vendor.ocr.FormValidator
import com.app.aisheildme.bytrix.tier4_vendor.ocr.MLKitOCRManager
import com.app.aisheildme.bytrix.tier4_vendor.ocr.ValidationIssue
import com.app.aisheildme.bytrix.tier4_vendor.ocr.VaultChange
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class DocumentAnalysis(
    val type: DocumentType,
    val confidence: Double,
    val text: String,
    val fields: List<ExtractedField>,
    val issues: List<ValidationIssue>,
    val summary: String?,
    val aiAssisted: Boolean,
    val pageCount: Int,
)

data class ImportOutcome(val saved: List<String>, val skipped: List<String>, val duplicate: Boolean)

/**
 * Workflow D (document import): Tier 4 reads the document (ML Kit OCR), Tier 3 classifies and
 * maps it (rules first, Gemini Nano to fill gaps), Tier 1 shows the proposal, and only after the
 * user approves does Tier 2 validate, de-duplicate and store it.
 */
@Singleton
class DocumentImportService @Inject constructor(
    private val ocr: MLKitOCRManager,
    private val nano: GeminiNanoManager,
    private val identity: VaultIdentity,
    private val documents: VaultDocumentRepository,
    private val healthcare: HealthcareRepository,
    private val retail: RetailRepository,
    private val banking: BankingRepository,
    private val audit: AuditLogger,
) {
    suspend fun analyze(pages: List<Uri>, fromScanner: Boolean): DocumentAnalysis {
        val read = ocr.recognize(pages, fromScanner)
        return analyzeText(read.text, read.pageCount)
    }

    suspend fun analyzeText(text: String, pageCount: Int = 1): DocumentAnalysis {
        var classification = DocumentClassifier.classify(text)
        var aiAssisted = false
        var fields = FormFieldExtractor.extract(text, classification.type)

        val expected = FormFieldExtractor.keysFor(classification.type)
        val missing = expected.filter { key -> fields.none { it.key == key } && key != FormFieldExtractor.TRANSACTIONS }
        if (missing.isNotEmpty()) {
            nano.extractFields(text, missing)?.let { suggested ->
                // AI values are suggestions: lower confidence, and they still face validation + review.
                fields = fields + suggested.map { (k, v) -> ExtractedField(k, v, 0.5) }
                aiAssisted = suggested.isNotEmpty()
            }
        }
        val summary = if (text.length > 200) nano.summarize(text, 40) else null
        if (summary != null) aiAssisted = true
        return DocumentAnalysis(
            type = classification.type,
            confidence = classification.confidence,
            text = text,
            fields = fields,
            issues = FormValidator.validate(classification.type, fields),
            summary = summary,
            aiAssisted = aiAssisted,
            pageCount = pageCount,
        )
    }

    fun proposedChanges(type: DocumentType, reviewed: List<ExtractedField>): List<VaultChange> =
        FormMapper.toVaultChanges(type, reviewed)

    /** Called only after the user approved [reviewed]. Re-validates; blocking issues stop the import. */
    suspend fun commit(type: DocumentType, text: String, reviewed: List<ExtractedField>, sourceUri: String?): ImportOutcome {
        val userId = identity.requireUserId()
        val blocking = FormValidator.validate(type, reviewed).filter { it.blocking }
        if (blocking.isNotEmpty()) throw VaultValidationException(blocking.joinToString { "${FormMapper.label(it.key)}: ${it.message}" })

        val hash = sha256("${type.name}|${text.lowercase().replace(Regex("\\s+"), " ").trim()}")
        val doc = VendorDocumentEntity(
            userId = userId, domain = type.domain.name, documentType = type.name,
            title = type.label, mimeType = "image/*", localUri = sourceUri, contentHash = hash, ocrText = text.take(20_000),
        )
        val saveResult = documents.saveReviewed(doc, reviewed)
        if (saveResult is DocumentSaveResult.Duplicate) {
            audit.log(action = "DOCUMENT_IMPORT", outcome = "DUPLICATE", domain = type.domain.name, entityType = "document", entityRef = saveResult.existingId.toString())
            return ImportOutcome(emptyList(), listOf("This document is already in your vault."), duplicate = true)
        }
        val documentId = (saveResult as DocumentSaveResult.Saved).documentId
        val saved = mutableListOf("Document stored")
        val skipped = mutableListOf<String>()

        for (change in FormMapper.toVaultChanges(type, reviewed)) {
            when (change) {
                is VaultChange.PatientDetails -> {
                    val existing = healthcare.patient(userId)
                    val first = change.firstName ?: existing?.firstName
                    val last = change.lastName ?: existing?.lastName
                    if (first.isNullOrBlank() || last.isNullOrBlank()) { skipped += "Patient details (name missing)"; continue }
                    healthcare.savePatient(
                        PatientEntity(
                            userId = userId, firstName = first, lastName = last,
                            dateOfBirth = change.dateOfBirth ?: existing?.dateOfBirth,
                            allergies = change.allergies ?: existing?.allergies,
                            sex = existing?.sex, bloodType = existing?.bloodType, conditions = existing?.conditions, fhirId = existing?.fhirId,
                        ),
                    )
                    saved += change.description
                }
                is VaultChange.Insurance -> {
                    val patient = healthcare.patient(userId)
                    if (patient == null) { skipped += "Insurance (add patient details first)"; continue }
                    val id = healthcare.addInsurance(InsurancePolicyEntity(patientId = patient.id, payerName = change.payer, memberId = change.memberId, groupNumber = change.group))
                    if (id == null) skipped += "Insurance already on file" else saved += change.description
                }
                is VaultChange.Medications -> {
                    val patient = healthcare.patient(userId)
                    if (patient == null) { skipped += "Medications (add patient details first)"; continue }
                    change.names.forEach { name ->
                        val parts = Regex("^(.*?)\\s+(\\d.*)$").find(name)
                        healthcare.addMedication(MedicationEntity(patientId = patient.id, name = parts?.groupValues?.get(1) ?: name, dosage = parts?.groupValues?.get(2).orEmpty()))
                    }
                    saved += change.description
                }
                is VaultChange.Purchase -> {
                    val id = retail.addPurchase(userId, NewPurchase(merchantName = change.merchant, orderNumber = change.orderNumber, purchasedAt = change.purchasedAt, totalMinor = change.totalMinor, receiptDocumentId = documentId))
                    if (id == null) skipped += "Purchase already on file" else saved += change.description
                }
                is VaultChange.Statement -> {
                    val accountId = banking.ensureAccount(userId, change.institution, "CHECKING", change.last4, "${change.institution} ••${change.last4}")
                    if (change.periodStart != null && change.periodEnd != null) {
                        banking.addStatement(BankStatementEntity(accountId = accountId, periodStart = change.periodStart, periodEnd = change.periodEnd, documentId = documentId))
                    }
                    val result = banking.importTransactions(accountId, change.transactions)
                    saved += "${result.inserted} transaction(s) added"
                    if (result.duplicates > 0) skipped += "${result.duplicates} duplicate transaction(s) skipped"
                }
            }
        }
        audit.log(action = "DOCUMENT_IMPORT", outcome = "SUCCESS", domain = type.domain.name, entityType = "document", entityRef = documentId.toString())
        return ImportOutcome(saved, skipped, duplicate = false)
    }

    private fun sha256(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
