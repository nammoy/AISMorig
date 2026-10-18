package com.app.aisheildme.bytrix.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.data.remote.api.DocumentApi
import com.app.aisheildme.bytrix.data.remote.dto.CreateCategoryRequest
import com.app.aisheildme.bytrix.data.remote.dto.RegisterDocumentRequest
import com.app.aisheildme.bytrix.data.remote.dto.ShareDocumentRequest
import com.app.aisheildme.bytrix.data.remote.dto.UploadUrlRequest
import com.app.aisheildme.bytrix.data.remote.dto.toDomain
import com.app.aisheildme.bytrix.domain.model.DocumentCategory
import com.app.aisheildme.bytrix.domain.model.DocumentItem
import com.app.aisheildme.bytrix.domain.model.FolderCategory
import com.app.aisheildme.bytrix.domain.model.ShareableUser
import com.app.aisheildme.bytrix.domain.model.UploadTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val api: DocumentApi,
    @param:ApplicationContext private val context: android.content.Context,
) {

    suspend fun getCategories(page: Int = 1, limit: Int = 20): List<DocumentCategory> =
        api.getCategories(page, limit).data.orEmpty().map { it.toDomain() }

    suspend fun createCategory(
        name: String,
        category: FolderCategory,
        parentId: Int? = null,
        sortOrder: Int? = null,
    ): String = api.createCategory(
        CreateCategoryRequest(
            name = name,
            icon = category.name.lowercase(),
            color = category.colorHex,
            parentId = parentId,
            sortOrder = sortOrder,
        ),
    ).message

    suspend fun getDocuments(
        userId: Int? = null,
        categoryId: Int? = null,
        page: Int = 1,
        limit: Int = 20,
    ): List<DocumentItem> =
        api.getDocuments(userId, categoryId, page, limit).data.orEmpty().map { it.toDomain() }

    suspend fun deleteDocument(id: Int): String = api.deleteDocument(id).message

    suspend fun getShareableUsers(page: Int = 1, limit: Int = 50): List<ShareableUser> =
        api.getShareableUsers(page = page, limit = limit).data.orEmpty().map { it.toDomain() }

    suspend fun shareDocument(documentId: Int, emails: List<String>): String =
        api.shareDocument(ShareDocumentRequest(documentId, emails)).message

    private suspend fun getUploadTarget(fileName: String, fileType: String?): UploadTarget =
        api.getUploadUrl(UploadUrlRequest(fileName = fileName, fileType = fileType))
            .data?.toDomain()
            ?: throw AppException("Could not start the upload.")

    suspend fun uploadDocument(
        title: String,
        uri: Uri,
        fileName: String,
        fileType: String,
        categoryId: Int? = null,
        description: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw AppException("Could not read the selected file.")

        val target = getUploadTarget(fileName, fileType)
        val headers = target.headers.ifEmpty { mapOf("Content-Type" to target.contentType) }

        api.putToSignedUrl(
            url = target.uploadUrl,
            headers = headers,
            body = bytes.toRequestBody(target.contentType.toMediaTypeOrNull()),
        ).close()

        api.registerDocument(
            RegisterDocumentRequest(
                title = title,
                encryptedFileUrl = target.fileUrl,
                s3Key = target.s3Key,
                fileName = fileName,
                fileType = fileType,
                fileSize = bytes.size.toLong(),
                categoryId = categoryId,
                description = description?.takeIf { it.isNotBlank() },
            ),
        ).message
    }
}
