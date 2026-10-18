package com.app.aisheildme.bytrix.domain.model

data class DocumentCategory(
    val id: Int = 0,
    val parentId: Int? = null,
    val name: String = "",
    val slug: String = "",
    val icon: String = "",
    val color: String = "",
    val isDefault: Boolean = false,
    val isOwner: Boolean = false,
    val sortOrder: Int = 0,
)

data class DocumentItem(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val documentType: String? = null,
    val category: String = "",
    val categoryId: Int? = null,
    val categoryIcon: String = "",
    val categoryColor: String = "",
    val tags: List<String> = emptyList(),
    val fileName: String = "",
    val fileType: String = "",
    val fileSize: Long = 0,
    val fileUrl: String = "",
    val s3Key: String = "",
    val createdAtEpochMs: Long? = null,
) {
    val isImage: Boolean get() = fileType.startsWith("image/")
    val isPdf: Boolean get() = fileType.contains("pdf", ignoreCase = true)
}

enum class FolderCategory(val label: String, val colorHex: String) {
    Address("Address", "#2D9CDB"),
    Contact("Contact", "#27AE60"),
    Payment("Payment", "#F2994A"),
    Health("Health", "#EB5757"),
    Other("Other", "#9B51E0"),
}

data class UploadTarget(
    val uploadUrl: String,
    val method: String = "PUT",
    val headers: Map<String, String> = emptyMap(),
    val s3Key: String = "",
    val fileUrl: String = "",
    val contentType: String = "application/octet-stream",
)

data class ShareableUser(
    val id: Int = 0,
    val firstname: String = "",
    val lastname: String = "",
    val email: String = "",
    val imageUrl: String = "",
) {
    val fullName: String get() = "$firstname $lastname".trim().ifEmpty { email }
}
