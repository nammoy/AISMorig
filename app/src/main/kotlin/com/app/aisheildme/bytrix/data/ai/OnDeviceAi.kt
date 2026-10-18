package com.app.aisheildme.bytrix.data.ai

import android.os.Build
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

enum class OnDeviceAiStatus {
    Unavailable,

    Downloadable,

    Downloading,

    Ready;

    val canBecomeReady: Boolean
        get() = this == Ready || this == Downloadable || this == Downloading
}

data class AiDownloadProgress(
    val bytesDownloaded: Long = 0,
    val totalBytes: Long? = null,
) {
    val fraction: Float?
        get() {
            val total = totalBytes ?: return null
            if (total <= 0) return null
            return (bytesDownloaded.toFloat() / total).coerceIn(0f, 1f)
        }

    val label: String
        get() = fraction?.let {
            "Downloading the model… ${(it * 100).toInt()}%"
        } ?: "Downloading the model…"
}

data class AiSummary(
    val text: String,
    val deviceLabel: String,
    val generatedAtEpochMs: Long = System.currentTimeMillis(),
    val modelLabel: String = "Gemini Nano",
) {
    val provenance: String get() = "Processed on-device on $deviceLabel"
}

class OnDeviceAiException(
    override val message: String,
    val status: OnDeviceAiStatus? = null,
    val cause0: String? = null,
) : Exception(message)

@Singleton
class OnDeviceAi @Inject constructor() {

    private val clients = mutableMapOf<Int, GenerativeModel>()

    @Volatile
    private var chosenPreference: Int? = null

    @Volatile
    var modelVariant: String? = null
        private set

    private val downloadLock = Mutex()

    private val _downloadProgress = MutableStateFlow(AiDownloadProgress())
    val downloadProgress: StateFlow<AiDownloadProgress> = _downloadProgress.asStateFlow()

    private fun client(preference: Int): GenerativeModel = clients.getOrPut(preference) {
        Generation.getClient(
            generationConfig {
                modelConfig = modelConfig {
                    releaseStage = ModelReleaseStage.STABLE
                    this.preference = preference
                }
            },
        )
    }

    private suspend fun statusOf(preference: Int): Int = try {
        client(preference).checkStatus()
    } catch (e: Throwable) {
        FeatureStatus.UNAVAILABLE
    }

    private suspend fun resolve(): Pair<Int, Int> {
        chosenPreference?.let { return it to statusOf(it) }

        var fallback: Pair<Int, Int>? = null
        for (preference in PREFERENCES) {
            val status = statusOf(preference)
            if (status == FeatureStatus.AVAILABLE) {
                chosenPreference = preference
                modelVariant = preferenceName(preference)
                return preference to status
            }
            if (status != FeatureStatus.UNAVAILABLE && fallback == null) {
                fallback = preference to status
            }
        }

        fallback?.let {
            chosenPreference = it.first
            modelVariant = preferenceName(it.first)
            return it
        }
        return ModelPreference.FULL to FeatureStatus.UNAVAILABLE
    }

    suspend fun status(): OnDeviceAiStatus = try {
        toStatus(resolve().second)
    } catch (e: Throwable) {
        OnDeviceAiStatus.Unavailable
    }

    suspend fun ensureReady(): OnDeviceAiStatus = downloadLock.withLock {
        var current = status()
        if (!current.canBecomeReady || current == OnDeviceAiStatus.Ready) return current

        try {
            download()
        } catch (e: Throwable) {
            throw OnDeviceAiException(
                "The model download did not finish. Check your connection and try again.",
                status = OnDeviceAiStatus.Downloadable,
                cause0 = e.message,
            )
        } finally {
            _downloadProgress.value = AiDownloadProgress()
        }

        current = status()
        return current
    }

    private suspend fun download() {
        var failure: Throwable? = null
        var total: Long? = null

        client(resolve().first).download().collect { status ->
            when (status) {
                is DownloadStatus.DownloadStarted -> {
                    total = status.bytesToDownload
                    _downloadProgress.value = AiDownloadProgress(0, total)
                }

                is DownloadStatus.DownloadProgress ->
                    _downloadProgress.value =
                        AiDownloadProgress(status.totalBytesDownloaded, total)

                is DownloadStatus.DownloadFailed -> failure = status.e

                else -> Unit
            }
        }
        failure?.let { throw it }
    }

    suspend fun generate(prompt: String): String {
        val ready = ensureReady()
        if (ready != OnDeviceAiStatus.Ready) {
            throw OnDeviceAiException(unavailableMessage(ready), status = ready)
        }

        val candidates = try {
            val request = generateContentRequest(TextPart(truncate(prompt))) {
                temperature = TEMPERATURE
                candidateCount = 1
                maxOutputTokens = MAX_OUTPUT_TOKENS
            }
            client(resolve().first).generateContent(request).candidates.map { it.text }
        } catch (e: Throwable) {
            throw OnDeviceAiException(
                "The on-device model could not finish: ${e.message ?: "unknown error"}",
                cause0 = e.javaClass.simpleName,
            )
        }

        return candidates.firstOrNull { !it.isNullOrBlank() }?.trim()
            ?: throw OnDeviceAiException(
                "The on-device model returned nothing. Please try again.",
            )
    }

    fun deviceLabel(): String = Build.MODEL?.trim()?.takeIf { it.isNotEmpty() } ?: "this device"

    suspend fun diagnostics(): String = listOf(
        "status=${status().name}",
        "variant=${modelVariant ?: "-"}",
        "device=${Build.MANUFACTURER}/${Build.MODEL}",
        "android=${Build.VERSION.RELEASE}",
        "sdk=${Build.VERSION.SDK_INT}",
    ).joinToString(" ")

    private fun truncate(prompt: String): String =
        if (prompt.length <= MAX_PROMPT_CHARS) {
            prompt
        } else {
            prompt.take(MAX_PROMPT_CHARS) + "\n[…truncated]"
        }

    private fun unavailableMessage(status: OnDeviceAiStatus): String = when (status) {
        OnDeviceAiStatus.Unavailable ->
            "This device does not have Gemini Nano available. Scanning still works — " +
                "the text below was read on-device."

        OnDeviceAiStatus.Downloading ->
            "The model is still downloading. Try again once it finishes."

        OnDeviceAiStatus.Downloadable ->
            "The model still needs to be downloaded. Try again on Wi-Fi."

        OnDeviceAiStatus.Ready -> "The on-device model is unavailable."
    }

    private fun toStatus(raw: Int): OnDeviceAiStatus = when (raw) {
        FeatureStatus.AVAILABLE -> OnDeviceAiStatus.Ready
        FeatureStatus.DOWNLOADABLE -> OnDeviceAiStatus.Downloadable
        FeatureStatus.DOWNLOADING -> OnDeviceAiStatus.Downloading
        else -> OnDeviceAiStatus.Unavailable
    }

    private fun preferenceName(preference: Int): String =
        if (preference == ModelPreference.FAST) "fast" else "full"

    companion object {
        private val PREFERENCES = listOf(ModelPreference.FULL, ModelPreference.FAST)

        const val MAX_PROMPT_CHARS = 4000

        private const val TEMPERATURE = 0.2f

        private const val MAX_OUTPUT_TOKENS = 256
    }
}
