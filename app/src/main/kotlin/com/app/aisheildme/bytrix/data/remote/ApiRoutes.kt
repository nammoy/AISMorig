package com.app.aisheildme.bytrix.data.remote

object ApiRoutes {
    const val LOGIN = "api/user/login"
    const val SOCIAL_LOGIN = "api/user/social-login"
    const val VERIFY_OTP_REGISTER = "api/user/verify-otp/register"
    const val SEND_OTP = "api/user/send-otp/mail"
    const val VERIFY_OTP_FORGOT_PASSWORD = "api/user/verify-otp/forgot-password"
    const val SET_NEW_PASSWORD = "api/user/set-password"
    const val CHANGE_PASSWORD = "api/user/change-password"
    const val LOGOUT = "api/user/logout"

    const val SET_PIN = "api/user/set-pin"
    const val VERIFY_PIN = "api/user/verify-pin"
    const val RESET_PIN = "api/user/reset-pin"

    const val USER = "api/user"
    const val USER_UNAVAILABILITY = "api/user/user-unavailability"
    const val PATIENT_PROFILE = "api/user/patient-profile"
    const val PATIENT_PROFILE_COMPLETE = "api/user/patient-profile/complete"
    const val UPLOAD_ATTACHMENT = "api/user/upload-attachments"
    const val DEVICE_TOKEN = "api/user/update-device-token"

    const val STATIC_CONTENT = "api/user/page"
    const val FAQS = "api/user/faq"
    const val NOTIFICATIONS = "api/user/notifications"
    const val TOGGLE_NOTIFICATION = "api/user/toggle-notification"

    const val DOCUMENT = "api/user/document"
    const val DOCUMENT_CATEGORY = "api/user/document-category"
    const val DOCUMENT_UPLOAD = "api/user/document/upload"
    const val DOCUMENT_PERMISSION_SHARE = "api/user/document-permission/share"
    const val DOCUMENT_REQUEST = "api/user/document-request"
    const val UPLOAD_URL = "api/user/upload-url"
    const val USERS = "api/user/users"

    const val RECORD = "api/user/record"
    const val RETAIL = "api/user/retail"
    const val HEALTH = "api/user/health"
    const val BANKING = "api/user/banking"

    const val MARK_SINGLE_READ = "api/user/mark-single-read/"

    const val BOOKKEEPER_INVOICE = "api/user/bookkeeper/invoice"
    const val PATIENT_REVIEW = "api/user/doctor/patient-review/"
    const val INVITE_FAMILY_CARE = "api/user/invite-family-care"
    const val INVITE_PATIENT = "api/user/invite-patient"
}
