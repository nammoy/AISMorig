package com.app.aisheildme.bytrix.tier4_vendor.ocr

import android.net.Uri
import com.app.aisheildme.bytrix.data.ai.DocumentTextReader
import com.app.aisheildme.bytrix.data.ai.ScanSource
import javax.inject.Inject
import javax.inject.Singleton

/** On-device text recognition (Google ML Kit, Latin script). Output is untrusted data. */
@Singleton
class MLKitOCRManager @Inject constructor(
    private val reader: DocumentTextReader,
) {
    data class OcrResult(val text: String, val pageCount: Int, val readable: Boolean)

    suspend fun recognize(pages: List<Uri>, fromScanner: Boolean): OcrResult {
        val doc = reader.readText(pages, if (fromScanner) ScanSource.DocumentScanner else ScanSource.Gallery)
        return OcrResult(text = clean(doc.rawText), pageCount = doc.pageCount, readable = doc.looksReadable)
    }

    companion object {
        /** Strips control characters and caps length; recognized text is never executed or obeyed. */
        fun clean(text: String): String = text
            .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]"), "")
            .replace(Regex("[ \\t]+"), " ")
            .take(20_000)
            .trim()
    }
}
