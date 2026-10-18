package com.app.aisheildme.bytrix.navigation

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

sealed interface Destination

@Serializable
data object SplashRoute : Destination

@Serializable
data object OnboardingRoute : Destination

@Serializable
data object LoginRoute : Destination

@Serializable
data object SignupRoute : Destination

@Serializable
data class BasicInfoRoute(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
) : Destination

@Keep
enum class VerifyFlow { Signup, Reset }

@Serializable
data class VerifyEmailRoute(
    val email: String,
    val flow: VerifyFlow,
) : Destination

@Serializable
data object SetupProfileRoute : Destination

@Serializable
data object CreateVaultPinRoute : Destination

@Serializable
data class ResetPasswordRoute(val token: String? = null) : Destination {
    val isChangeMode: Boolean get() = token == null
}

@Serializable
data object DashboardRoute : Destination

@Serializable
data class FoldersRoute(val openCreate: Boolean = false) : Destination

@Serializable
data class DocumentsRoute(
    val folderName: String,
    val categoryId: Int = -1,
) : Destination

@Serializable
data class ShareRoute(
    val documentId: Int,
    val title: String = "",
) : Destination

@Serializable
data object NotificationRoute : Destination

@Serializable
data object HealthcareRoute : Destination

@Serializable
data object AppointmentDetailRoute : Destination

@Serializable
data object RetailRoute : Destination

@Serializable
data object RetailDetailRoute : Destination

@Serializable
data object BankingRoute : Destination

@Serializable
data object BankStatementDetailRoute : Destination

@Serializable
data object CheckInHistoryRoute : Destination

@Serializable
data object AddInvoiceRoute : Destination

@Serializable
data object EditProfileRoute : Destination

@Serializable
data object ResetPinRoute : Destination

@Serializable
data object AvailabilityRoute : Destination

@Serializable
data class StaticPageRoute(val slug: String = "privacy-policy") : Destination

@Serializable
data object CheckInFormRoute : Destination

@Serializable
data object CheckInPreviewRoute : Destination

@Serializable
data object CheckInReportRoute : Destination

// ---- Four-tier AI-ShieldMe destinations (Tier 1) ----

@Serializable
data object AssistantRoute : Destination

@Serializable
data object PersonalVaultRoute : Destination

@Serializable
data object DocumentImportRoute : Destination

/** tab: 0 requests, 1 consents, 2 history */
@Serializable
data class ActivityRoute(val tab: Int = 0) : Destination
