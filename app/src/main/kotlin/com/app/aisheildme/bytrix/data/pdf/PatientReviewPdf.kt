package com.app.aisheildme.bytrix.data.pdf

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.app.aisheildme.bytrix.core.util.TimeFormat
import com.app.aisheildme.bytrix.domain.model.FindingSeverity
import com.app.aisheildme.bytrix.domain.model.PatientReview
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientReviewPdfGenerator @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    suspend fun build(review: PatientReview, patientName: String): File =
        withContext(Dispatchers.IO) {
            val document = android.graphics.pdf.PdfDocument()
            val page = ReviewPageRenderer(document)

            page.heading("Patient Review")
            page.caption("$patientName · ${TimeFormat.longDateTime(System.currentTimeMillis())}")

            page.section("Patient")
            page.field("Code", review.patient.patientCode)
            page.field("Name", review.patient.name)
            page.field("Date of birth", review.patient.dob)
            page.field("Gender", review.patient.gender)
            page.field("Blood type", review.patient.bloodType)
            page.field("Primary doctor", review.patient.primaryDoctor)

            review.aiAgent?.let { agent ->
                page.section("Clinical assessment")
                agent.summary?.let(page::paragraph)
                page.field("Recommendation", agent.recommendation)
                page.field("Confidence", agent.confidenceLabel)
                page.field("Driver", agent.driver)
            }

            page.section("Findings")
            if (review.findings.isEmpty()) {
                page.paragraph("No issues were flagged.")
            } else {
                page.field(
                    "Counts",
                    "${review.summary.critical} critical · " +
                        "${review.summary.warning} warning · ${review.summary.info} info",
                )
                review.findings.forEach { finding ->
                    val tag = when (finding.severity) {
                        FindingSeverity.Critical -> "CRITICAL"
                        FindingSeverity.Warning -> "WARNING"
                        FindingSeverity.Info -> "INFO"
                    }
                    page.bullet("[$tag] ${finding.message ?: finding.code ?: "—"}")
                }
            }

            review.vitalSigns?.let { vitals ->
                page.section("Vitals")
                page.field("Blood pressure", vitals.bloodPressure)
                page.field("Heart rate", vitals.heartRate)
                page.field("Temperature", vitals.temperature)
                page.field("Oxygen", vitals.oxygen)
                page.field("BMI", vitals.bmi)
            }

            if (review.medicalConditions.isNotEmpty()) {
                page.section("Conditions")
                review.medicalConditions.forEach(page::bullet)
            }

            if (review.allergies.isNotEmpty()) {
                page.section("Allergies")
                review.allergies.forEach(page::bullet)
            }

            if (review.medications.isNotEmpty()) {
                page.section("Medications")
                review.medications.forEach {
                    page.bullet("${it.name} — ${it.dose} — ${it.frequency}")
                }
            }

            if (review.labResults.isNotEmpty()) {
                page.section("Lab results")
                review.labResults.forEach {
                    page.bullet("${it.testName}: ${it.resultValue} ${it.unit}".trim())
                }
            }

            page.finish()

            val exports = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exports, fileName(patientName))
            file.outputStream().use(document::writeTo)
            document.close()
            file
        }

    suspend fun saveToDownloads(source: File): String = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, source.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create the download entry.")

            resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val downloads =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloads.mkdirs()
            source.copyTo(File(downloads, source.name), overwrite = true)
        }
        "Saved to Downloads"
    }

    fun shareUri(file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    private fun fileName(patientName: String): String {
        val safe = patientName.replace(Regex("[^A-Za-z0-9]"), "").ifEmpty { "patient" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "patient_review_${safe}_$stamp.pdf"
    }
}

private class ReviewPageRenderer(private val document: android.graphics.pdf.PdfDocument) {

    private var page: android.graphics.pdf.PdfDocument.Page? = null
    private var canvas: android.graphics.Canvas? = null
    private var y = 0f
    private var pageNumber = 0

    private val body = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 10f
        isAntiAlias = true
    }
    private val label = android.graphics.Paint(body).apply { color = 0xFF707070.toInt() }
    private val sectionPaint = android.graphics.Paint(body).apply {
        textSize = 12f
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD,
        )
    }
    private val headingPaint = android.graphics.Paint(body).apply {
        textSize = 20f
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD,
        )
    }
    private val captionPaint = android.graphics.Paint(body).apply {
        textSize = 9f
        color = 0xFF9A9A9A.toInt()
    }

    init {
        newPage()
    }

    private fun newPage() {
        page?.let(document::finishPage)
        pageNumber++
        val info = android.graphics.pdf.PdfDocument.PageInfo
            .Builder(WIDTH, HEIGHT, pageNumber).create()
        page = document.startPage(info)
        canvas = page?.canvas
        y = MARGIN
    }

    private fun ensure(space: Float) {
        if (y + space > HEIGHT - MARGIN) newPage()
    }

    fun heading(text: String) {
        ensure(28f)
        canvas?.drawText(text, MARGIN, y, headingPaint)
        y += 26f
    }

    fun caption(text: String) {
        ensure(14f)
        canvas?.drawText(text, MARGIN, y, captionPaint)
        y += 16f
    }

    fun section(title: String) {
        ensure(28f)
        y += 10f
        canvas?.drawText(title.uppercase(), MARGIN, y, sectionPaint)
        y += 6f
        canvas?.drawLine(MARGIN, y, WIDTH - MARGIN, y, label)
        y += 12f
    }

    fun field(name: String, value: String?) {
        val v = value?.trim()?.takeIf { it.isNotEmpty() } ?: return
        ensure(14f)
        canvas?.drawText(name, MARGIN, y, label)
        canvas?.drawText(v, MARGIN + LABEL_WIDTH, y, body)
        y += 14f
    }

    fun bullet(text: String) = wrap("• $text")

    fun paragraph(text: String) = wrap(text)

    private fun wrap(text: String) {
        val maxWidth = WIDTH - MARGIN * 2
        var line = StringBuilder()
        for (word in text.split(' ')) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (body.measureText(candidate) > maxWidth) {
                ensure(13f)
                canvas?.drawText(line.toString(), MARGIN, y, body)
                y += 13f
                line = StringBuilder(word)
            } else {
                line = StringBuilder(candidate)
            }
        }
        if (line.isNotEmpty()) {
            ensure(13f)
            canvas?.drawText(line.toString(), MARGIN, y, body)
            y += 13f
        }
    }

    fun finish() {
        page?.let(document::finishPage)
        page = null
    }

    private companion object {
        const val WIDTH = 595
        const val HEIGHT = 842
        const val MARGIN = 40f
        const val LABEL_WIDTH = 140f
    }
}
