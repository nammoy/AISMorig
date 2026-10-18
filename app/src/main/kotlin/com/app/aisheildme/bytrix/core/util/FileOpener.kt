package com.app.aisheildme.bytrix.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileOpener @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun downloadAndOpen(
        context: Context,
        url: String,
        fileName: String,
        mimeType: String?,
    ): String? = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext "File not available."

        try {
            val target = File(context.cacheDir, "documents").apply { mkdirs() }
                .resolve(fileName.ifEmpty { "document" }.replace(Regex("\\s+"), "_"))

            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext "Could not download the document."
                response.body?.byteStream()?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext "Could not download the document."
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                target,
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType?.takeIf { it.isNotBlank() } ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            withContext(Dispatchers.Main) {
                try {
                    context.startActivity(intent)
                    null
                } catch (e: android.content.ActivityNotFoundException) {
                    "No app on this device can open ${target.extension.uppercase()} files."
                }
            }
        } catch (e: java.io.IOException) {
            "Could not open the document."
        }
    }
}

object MimeTypes {
    fun forFileName(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
}
