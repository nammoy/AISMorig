package com.app.aisheildme.bytrix.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.app.aisheildme.bytrix.domain.model.FolderCategory

object CategoryIcons {

    fun forKey(key: String?): ImageVector = when (key?.lowercase()?.trim()) {
        "address", "maps", "location" -> Icons.Default.LocationOn
        "contact", "contacts", "user" -> Icons.Default.Person
        "payment", "invoice", "billing" -> Icons.AutoMirrored.Filled.ReceiptLong
        "health", "healthcare", "medical" -> Icons.Default.Favorite
        "retail", "shopping" -> Icons.Default.ShoppingBasket
        "bank", "banking" -> Icons.Default.AccountBalance
        "hospital" -> Icons.Default.LocalHospital
        else -> Icons.Default.Folder
    }

    fun forFolderCategory(category: FolderCategory): ImageVector = when (category) {
        FolderCategory.Address -> Icons.Default.LocationOn
        FolderCategory.Contact -> Icons.Default.Person
        FolderCategory.Payment -> Icons.AutoMirrored.Filled.ReceiptLong
        FolderCategory.Health -> Icons.Default.Favorite
        FolderCategory.Other -> Icons.Default.Folder
    }

    fun parseColor(hex: String?, fallback: Color = AppColors.BrandBlue): Color {
        val cleaned = hex?.trim()?.removePrefix("#") ?: return fallback
        if (cleaned.length != 6 && cleaned.length != 8) return fallback
        val value = cleaned.toLongOrNull(16) ?: return fallback
        return if (cleaned.length == 6) Color(value or 0xFF000000) else Color(value)
    }
}
