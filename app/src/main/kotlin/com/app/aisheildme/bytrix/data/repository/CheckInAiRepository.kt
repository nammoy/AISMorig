package com.app.aisheildme.bytrix.data.repository

import com.app.aisheildme.bytrix.data.ai.AiSummary
import com.app.aisheildme.bytrix.data.ai.CheckInPrompts
import com.app.aisheildme.bytrix.data.ai.OnDeviceAi
import com.app.aisheildme.bytrix.data.ai.OnDeviceAiException
import com.app.aisheildme.bytrix.domain.model.ScannedFormData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInAiRepository @Inject constructor(
    private val onDeviceAi: OnDeviceAi,
) {
    val downloadProgress = onDeviceAi.downloadProgress

    suspend fun status() = onDeviceAi.status()

    suspend fun diagnostics() = onDeviceAi.diagnostics()

    suspend fun summarizeScan(scannedText: String): AiSummary {
        val trimmed = scannedText.trim()
        if (trimmed.isEmpty()) throw OnDeviceAiException("There was no text to summarise.")
        return AiSummary(
            text = onDeviceAi.generate(CheckInPrompts.scanSummary(trimmed)),
            deviceLabel = onDeviceAi.deviceLabel(),
        )
    }

    suspend fun extractFields(scannedText: String): ScannedFormData {
        val trimmed = scannedText.trim()
        if (trimmed.isEmpty()) throw OnDeviceAiException("There was no text to read.")
        return ScannedFormData.parse(onDeviceAi.generate(CheckInPrompts.extraction(trimmed)))
    }

    suspend fun summarizeForm(formText: String): AiSummary = AiSummary(
        text = onDeviceAi.generate(CheckInPrompts.formSummary(formText.trim())),
        deviceLabel = onDeviceAi.deviceLabel(),
    )
}
