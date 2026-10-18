package com.app.aisheildme.bytrix.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.app.aisheildme.bytrix.core.util.TimeFormat
import com.app.aisheildme.bytrix.domain.model.CheckInForm
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInPdfGenerator @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun generate(form: CheckInForm): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val renderer = PageRenderer(document)

        renderer.heading("Patient Check-In")
        renderer.caption(
            "${form.clinicName.takeIf { it != "—" } ?: "Clinic"} · " +
                TimeFormat.longDateTime(form.checkInAtEpochMs),
        )
        renderer.gap()

        renderer.section("Patient")
        renderer.field("Patient ID", form.patientId)
        renderer.field("Name", form.fullName)
        renderer.field("Date of birth", form.dateOfBirth)
        renderer.field("Age", form.age)
        renderer.field("Gender", form.gender)
        renderer.field("Blood type", form.bloodType)
        renderer.field("Phone", form.phone)
        renderer.field("Email", form.email)
        renderer.field("Address", form.address)

        renderer.section("Visit")
        renderer.field("Appointment", form.appointmentLabel)
        renderer.field("Reason", form.reasonForVisit)
        renderer.field("Symptoms", form.symptomSummary)
        renderer.field("Pain level", "${form.painLevel}/10")

        renderer.section("Emergency contact")
        renderer.field("Name", form.emergencyName)
        renderer.field("Relation", form.emergencyRelation)
        renderer.field("Phone", form.emergencyPhone)

        renderer.section("Insurance")
        renderer.field("Company", form.insuranceCompany)
        renderer.field("Member ID", form.memberId)
        renderer.field("Group", form.groupNumber)
        renderer.field("Primary physician", form.primaryCarePhysician)

        renderer.section("Medical record on file")
        renderer.field("Conditions", form.knownConditions)
        renderer.field("Allergies", form.knownAllergies)
        renderer.field("Medications", form.knownMedications)
        if (form.hasNewAllergies) {
            renderer.field("New allergies", form.newAllergyDetail)
        }

        renderer.section("Vitals")
        renderer.field("Blood pressure", form.bloodPressure)
        renderer.field("Pulse", form.pulse)
        renderer.field("Temperature", form.temperature)
        renderer.field("Oxygen saturation", form.oxygenSaturation)
        renderer.field("Weight", form.weight)
        renderer.field("Height", form.height)
        renderer.field("BMI", form.bmi)

        if (form.filledMedications.isNotEmpty()) {
            renderer.section("Current medications")
            form.filledMedications.forEach {
                renderer.bullet("${it.name} — ${it.dose} — ${it.frequency}")
            }
        }

        if (form.filledLabResults.isNotEmpty()) {
            renderer.section("Lab results")
            form.filledLabResults.forEach {
                renderer.bullet("${it.testName}: ${it.resultValue} ${it.unit}".trim())
            }
        }

        renderer.section("Pharmacy")
        renderer.field("Name", form.pharmacyName)
        renderer.field("Phone", form.pharmacyPhone)

        form.otherDetails?.trim()?.takeIf { it.isNotEmpty() && it != "—" }?.let {
            renderer.section("Other details")
            renderer.paragraph(it)
        }

        if (form.hasAiSummary) {
            renderer.section("AI summary")
            renderer.paragraph(form.aiSummary.orEmpty())
            form.aiSummaryDevice?.let {
                renderer.caption("Processed on-device on $it")
            }
        }

        renderer.section("Consent")
        renderer.paragraph(
            if (form.consentGiven) {
                "The patient confirmed the consent statement."
            } else {
                "Consent was not confirmed."
            },
        )
        form.signature?.let { renderer.signature(it.png) }

        renderer.finish()

        val exports = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exports, "check-in-${form.checkInAtEpochMs}.pdf")
        file.outputStream().use(document::writeTo)
        document.close()
        file
    }
}

private class PageRenderer(private val document: PdfDocument) {

    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = 0f
    private var pageNumber = 0

    private val body = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 10f
        isAntiAlias = true
    }
    private val label = Paint(body).apply { color = 0xFF707070.toInt() }
    private val sectionPaint = Paint(body).apply {
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val headingPaint = Paint(body).apply {
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val captionPaint = Paint(body).apply {
        textSize = 9f
        color = 0xFF9A9A9A.toInt()
    }

    init {
        newPage()
    }

    private fun newPage() {
        page?.let(document::finishPage)
        pageNumber++
        val info = PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, pageNumber).create()
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
        y += 14f
    }

    fun section(title: String) {
        ensure(28f)
        y += 10f
        canvas?.drawText(title.uppercase(), MARGIN, y, sectionPaint)
        y += 6f
        canvas?.drawLine(MARGIN, y, WIDTH - MARGIN, y, label)
        y += 12f
    }

    fun field(name: String, value: String) {
        ensure(14f)
        canvas?.drawText(name, MARGIN, y, label)
        canvas?.drawText(value.ifBlank { "—" }, MARGIN + LABEL_WIDTH, y, body)
        y += 14f
    }

    fun bullet(text: String) {
        wrap("• $text", MARGIN, WIDTH - MARGIN * 2)
    }

    fun paragraph(text: String) {
        wrap(text, MARGIN, WIDTH - MARGIN * 2)
    }

    fun gap() {
        y += 8f
    }

    private fun wrap(text: String, x: Float, maxWidth: Float) {
        var line = StringBuilder()
        for (word in text.split(' ')) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (body.measureText(candidate) > maxWidth) {
                ensure(13f)
                canvas?.drawText(line.toString(), x, y, body)
                y += 13f
                line = StringBuilder(word)
            } else {
                line = StringBuilder(candidate)
            }
        }
        if (line.isNotEmpty()) {
            ensure(13f)
            canvas?.drawText(line.toString(), x, y, body)
            y += 13f
        }
    }

    fun signature(png: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(png, 0, png.size) ?: return
        val targetWidth = 180f
        val scale = targetWidth / bitmap.width
        val targetHeight = bitmap.height * scale

        ensure(targetHeight + 20f)
        y += 8f
        canvas?.drawBitmap(
            bitmap,
            null,
            Rect(
                MARGIN.toInt(),
                y.toInt(),
                (MARGIN + targetWidth).toInt(),
                (y + targetHeight).toInt(),
            ),
            null,
        )
        y += targetHeight + 6f
        canvas?.drawText("Patient signature", MARGIN, y, captionPaint)
        y += 14f
        bitmap.recycle()
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
