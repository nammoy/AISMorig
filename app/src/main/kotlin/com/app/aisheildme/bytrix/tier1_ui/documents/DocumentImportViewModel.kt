package com.app.aisheildme.bytrix.tier1_ui.documents

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.tier2_vault.repository.ExtractedField
import com.app.aisheildme.bytrix.tier3_negotiator.DocumentAnalysis
import com.app.aisheildme.bytrix.tier3_negotiator.DocumentImportService
import com.app.aisheildme.bytrix.tier3_negotiator.ImportOutcome
import com.app.aisheildme.bytrix.tier4_vendor.ocr.DocumentType
import com.app.aisheildme.bytrix.tier4_vendor.ocr.FormFieldExtractor
import com.app.aisheildme.bytrix.tier4_vendor.ocr.FormValidator
import com.app.aisheildme.bytrix.tier4_vendor.ocr.ValidationIssue
import com.app.aisheildme.bytrix.tier4_vendor.ocr.VaultChange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ImportUi {
    data object Idle : ImportUi
    data object Reading : ImportUi
    data class Review(
        val analysis: DocumentAnalysis,
        val type: DocumentType,
        val fields: List<ExtractedField>,
        val issues: List<ValidationIssue>,
        val changes: List<VaultChange>,
        val sourceUri: String?,
        val saving: Boolean = false,
        val error: String? = null,
    ) : ImportUi {
        val canSave: Boolean get() = !saving && issues.none { it.blocking } && changes.isNotEmpty()
    }
    data class Done(val outcome: ImportOutcome) : ImportUi
    data class Failed(val message: String) : ImportUi
}

@HiltViewModel
class DocumentImportViewModel @Inject constructor(
    private val service: DocumentImportService,
) : ViewModel() {

    private val _ui = MutableStateFlow<ImportUi>(ImportUi.Idle)
    val ui: StateFlow<ImportUi> = _ui.asStateFlow()

    fun onPages(pages: List<Uri>, fromScanner: Boolean) {
        if (pages.isEmpty()) return
        _ui.value = ImportUi.Reading
        viewModelScope.launch {
            _ui.value = runCatching { service.analyze(pages, fromScanner) }.fold(
                onSuccess = { a ->
                    if (a.text.length < 10) ImportUi.Failed("No readable text was found. Try a sharper, well-lit photo.")
                    else review(a, a.type, a.fields, pages.first().toString())
                },
                onFailure = { ImportUi.Failed("Could not read the document on this device.") },
            )
        }
    }

    fun onTypeChange(type: DocumentType) = (_ui.value as? ImportUi.Review)?.let { r ->
        val fields = FormFieldExtractor.extract(r.analysis.text, type)
        _ui.value = review(r.analysis, type, fields, r.sourceUri)
    }

    fun onFieldChange(key: String, value: String) = (_ui.value as? ImportUi.Review)?.let { r ->
        val exists = r.fields.any { it.key == key }
        val fields = if (exists) {
            r.fields.map { if (it.key == key) it.copy(value = value.take(200), confidence = 1.0) else it }
        } else {
            r.fields + ExtractedField(key, value.take(200), 1.0)
        }
        _ui.value = review(r.analysis, r.type, fields, r.sourceUri)
    }

    fun approveAndSave() {
        val r = _ui.value as? ImportUi.Review ?: return
        if (!r.canSave) return
        _ui.value = r.copy(saving = true, error = null)
        viewModelScope.launch {
            _ui.value = runCatching { service.commit(r.type, r.analysis.text, r.fields, r.sourceUri) }.fold(
                onSuccess = { ImportUi.Done(it) },
                onFailure = { r.copy(saving = false, error = it.message ?: "Could not save.") },
            )
        }
    }

    fun reset() = _ui.update { ImportUi.Idle }

    private fun review(a: DocumentAnalysis, type: DocumentType, fields: List<ExtractedField>, uri: String?): ImportUi.Review {
        val keys = FormFieldExtractor.keysFor(type)
        val complete = keys.map { k -> fields.firstOrNull { it.key == k } ?: ExtractedField(k, "", 0.0) } +
            fields.filter { it.key !in keys }
        return ImportUi.Review(
            analysis = a, type = type, fields = complete,
            issues = FormValidator.validate(type, complete.filter { it.value.isNotBlank() }),
            changes = service.proposedChanges(type, complete.filter { it.value.isNotBlank() }),
            sourceUri = uri,
        )
    }
}
