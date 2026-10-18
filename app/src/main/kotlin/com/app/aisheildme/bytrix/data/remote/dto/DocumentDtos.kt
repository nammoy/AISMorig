package com.app.aisheildme.bytrix.data.remote.dto

import com.app.aisheildme.bytrix.core.util.IsoParser
import com.app.aisheildme.bytrix.data.remote.FlexibleBooleanSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleIntSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleStringSerializer
import com.app.aisheildme.bytrix.domain.model.DocumentCategory
import com.app.aisheildme.bytrix.domain.model.DocumentItem
import com.app.aisheildme.bytrix.domain.model.ShareableUser
import com.app.aisheildme.bytrix.domain.model.UploadTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DocumentCategoryDto(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @SerialName("parent_id") val parentId: Int? = null,
    val name: String = "",
    val slug: String = "",
    @Serializable(with = FlexibleStringSerializer::class) val icon: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val color: String? = null,
    @SerialName("is_default")
    @Serializable(with = FlexibleBooleanSerializer::class) val isDefault: Boolean = false,
    @SerialName("is_owner")
    @Serializable(with = FlexibleBooleanSerializer::class) val isOwner: Boolean = false,
    @SerialName("sort_order")
    @Serializable(with = FlexibleIntSerializer::class) val sortOrder: Int = 0,
)

fun DocumentCategoryDto.toDomain() = DocumentCategory(
    id = id,
    parentId = parentId,
    name = name,
    slug = slug,
    icon = icon.orEmpty(),
    color = color.orEmpty(),
    isDefault = isDefault,
    isOwner = isOwner,
    sortOrder = sortOrder,
)

@Serializable
data class DocumentDto(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    val title: String = "",
    @Serializable(with = FlexibleStringSerializer::class) val description: String? = null,
    @SerialName("document_type")
    @Serializable(with = FlexibleStringSerializer::class) val documentType: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val category: String? = null,
    @SerialName("category_id") val categoryId: Int? = null,
    @SerialName("category_detail") val categoryDetail: CategoryDetailDto? = null,
    val tags: List<String> = emptyList(),
    @SerialName("file_name") val fileName: String = "",
    @SerialName("file_type") val fileType: String = "",
    @SerialName("file_size") val fileSize: Long = 0,
    @SerialName("encrypted_file_url") val fileUrl: String = "",
    @SerialName("s3_key") val s3Key: String = "",
    @SerialName("createdAt")
    @Serializable(with = FlexibleStringSerializer::class) val createdAt: String? = null,
)

@Serializable
data class CategoryDetailDto(
    @Serializable(with = FlexibleStringSerializer::class) val name: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val icon: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val color: String? = null,
)

fun DocumentDto.toDomain() = DocumentItem(
    id = id,
    title = title,
    description = description.orEmpty(),
    documentType = documentType,
    category = category ?: categoryDetail?.name.orEmpty(),
    categoryId = categoryId,
    categoryIcon = categoryDetail?.icon.orEmpty(),
    categoryColor = categoryDetail?.color.orEmpty(),
    tags = tags,
    fileName = fileName,
    fileType = fileType,
    fileSize = fileSize,
    fileUrl = fileUrl,
    s3Key = s3Key,
    createdAtEpochMs = IsoParser.toEpochMs(createdAt),
)

@Serializable
data class UploadUrlDto(
    @SerialName("upload_url") val uploadUrl: String = "",
    val method: String = "PUT",
    val headers: Map<String, String> = emptyMap(),
    @SerialName("s3_key") val s3Key: String = "",
    @SerialName("file_url") val fileUrl: String = "",
    @SerialName("content_type") val contentType: String = "application/octet-stream",
    @SerialName("expires_in") val expiresIn: Int = 0,
)

fun UploadUrlDto.toDomain() = UploadTarget(
    uploadUrl = uploadUrl,
    method = method,
    headers = headers,
    s3Key = s3Key,
    fileUrl = fileUrl,
    contentType = contentType,
)

@Serializable
data class ShareableUserDto(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    val firstname: String = "",
    val lastname: String = "",
    @Serializable(with = FlexibleStringSerializer::class) val name: String? = null,
    val email: String = "",
    @SerialName("image_url")
    @Serializable(with = FlexibleStringSerializer::class) val imageUrl: String? = null,
)

fun ShareableUserDto.toDomain(): ShareableUser {
    val resolved = name?.takeIf { it.isNotBlank() } ?: "$firstname $lastname".trim()
    return ShareableUser(
        id = id,
        firstname = resolved.substringBefore(' '),
        lastname = resolved.substringAfter(' ', ""),
        email = email,
        imageUrl = imageUrl.orEmpty(),
    )
}

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("parent_id") val parentId: Int? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

@Serializable
data class UploadUrlRequest(
    @SerialName("file_name") val fileName: String,
    @SerialName("file_type") val fileType: String? = null,
    val path: String = "documents",
)

@Serializable
data class RegisterDocumentRequest(
    val title: String,
    @SerialName("encrypted_file_url") val encryptedFileUrl: String,
    @SerialName("s3_key") val s3Key: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_type") val fileType: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("category_id") val categoryId: Int? = null,
    val description: String? = null,
    @SerialName("document_type") val documentType: String? = null,
    val tags: List<String>? = null,
)

@Serializable
data class ShareDocumentRequest(
    @SerialName("document_id") val documentId: Int,
    val emails: List<String>,
)
