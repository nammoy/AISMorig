package com.app.aisheildme.bytrix.data.ai

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class ScanSource {
    DocumentScanner,

    Gallery,
}

data class ScannedDocument(
    val pages: List<Uri> = emptyList(),
    val rawText: String = "",
    val source: ScanSource = ScanSource.DocumentScanner,
) {
    val pageCount: Int get() = pages.size

    val hasText: Boolean get() = rawText.isNotBlank()

    val looksReadable: Boolean get() = rawText.trim().length >= 20
}

class ScanException(override val message: String) : Exception(message)

@Singleton
class DocumentTextReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun readText(pages: List<Uri>, source: ScanSource): ScannedDocument {
        if (pages.isEmpty()) throw ScanException("No pages were captured.")

        val texts = pages.mapNotNull { uri ->
            try {
                val image = InputImage.fromFilePath(context, uri)
                recognizer.process(image).await().text.takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                null
            }
        }

        return ScannedDocument(
            pages = pages,
            rawText = texts.joinToString("\n\n"),
            source = source,
        )
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}
