package com.app.aisheildme.bytrix.data.remote.api

import com.app.aisheildme.bytrix.data.remote.ApiRoutes
import com.app.aisheildme.bytrix.data.remote.dto.ApiResponse
import com.app.aisheildme.bytrix.data.remote.dto.BankStatementDto
import com.app.aisheildme.bytrix.data.remote.dto.CreateCategoryRequest
import com.app.aisheildme.bytrix.data.remote.dto.DocumentCategoryDto
import com.app.aisheildme.bytrix.data.remote.dto.DocumentDto
import com.app.aisheildme.bytrix.data.remote.dto.HealthAppointmentDto
import com.app.aisheildme.bytrix.data.remote.dto.InvoiceRequest
import com.app.aisheildme.bytrix.data.remote.dto.NotificationDto
import com.app.aisheildme.bytrix.data.remote.dto.PatientPrefillDto
import com.app.aisheildme.bytrix.data.remote.dto.PatientReviewDto
import com.app.aisheildme.bytrix.data.remote.dto.RegisterDocumentRequest
import com.app.aisheildme.bytrix.data.remote.dto.RetailOrderDto
import com.app.aisheildme.bytrix.data.remote.dto.ShareDocumentRequest
import com.app.aisheildme.bytrix.data.remote.dto.ShareableUserDto
import com.app.aisheildme.bytrix.data.remote.dto.StaticPageDto
import com.app.aisheildme.bytrix.data.remote.dto.ToggleNotificationRequest
import com.app.aisheildme.bytrix.data.remote.dto.UnavailabilityRequest
import com.app.aisheildme.bytrix.data.remote.dto.UploadUrlDto
import com.app.aisheildme.bytrix.data.remote.dto.UploadUrlRequest
import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface DocumentApi {

    @GET(ApiRoutes.DOCUMENT_CATEGORY)
    suspend fun getCategories(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<List<DocumentCategoryDto>>

    @POST(ApiRoutes.DOCUMENT_CATEGORY)
    suspend fun createCategory(@Body body: CreateCategoryRequest): ApiResponse<JsonElement>

    @GET(ApiRoutes.DOCUMENT)
    suspend fun getDocuments(
        @Query("user_id") userId: Int? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<List<DocumentDto>>

    @DELETE("${ApiRoutes.DOCUMENT}/{id}")
    suspend fun deleteDocument(@Path("id") id: Int): ApiResponse<JsonElement>

    @GET(ApiRoutes.USERS)
    suspend fun getShareableUsers(
        @Query("not_me") notMe: Int? = 1,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): ApiResponse<List<ShareableUserDto>>

    @POST(ApiRoutes.DOCUMENT_PERMISSION_SHARE)
    suspend fun shareDocument(@Body body: ShareDocumentRequest): ApiResponse<JsonElement>

    @POST(ApiRoutes.UPLOAD_URL)
    suspend fun getUploadUrl(@Body body: UploadUrlRequest): ApiResponse<UploadUrlDto>

    @POST(ApiRoutes.DOCUMENT_UPLOAD)
    suspend fun registerDocument(@Body body: RegisterDocumentRequest): ApiResponse<JsonElement>

    @retrofit2.http.PUT
    suspend fun putToSignedUrl(
        @Url url: String,
        @retrofit2.http.HeaderMap headers: Map<String, String>,
        @Body body: RequestBody,
    ): okhttp3.ResponseBody

    @Multipart
    @POST(ApiRoutes.UPLOAD_ATTACHMENT)
    suspend fun uploadAttachment(
        @Part("path") path: RequestBody,
        @Part file: MultipartBody.Part,
    ): ApiResponse<List<String>>
}

interface RecordApi {

    @GET(ApiRoutes.HEALTH)
    suspend fun getHealth(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<List<HealthAppointmentDto>>

    @GET(ApiRoutes.RETAIL)
    suspend fun getRetail(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<List<RetailOrderDto>>

    @GET(ApiRoutes.BANKING)
    suspend fun getBanking(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<List<BankStatementDto>>

    @GET(ApiRoutes.RECORD)
    suspend fun getRecords(
        @Query("industry") industry: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<JsonElement>
}

interface NotificationApi {

    @GET(ApiRoutes.NOTIFICATIONS)
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<List<NotificationDto>>

    @POST("${ApiRoutes.MARK_SINGLE_READ}{id}")
    suspend fun markRead(@Path("id") id: Int): ApiResponse<JsonElement>

    @POST("${ApiRoutes.DOCUMENT_REQUEST}/{id}/approve")
    suspend fun approveRequest(@Path("id") id: Int): ApiResponse<JsonElement>

    @POST("${ApiRoutes.DOCUMENT_REQUEST}/{id}/reject")
    suspend fun rejectRequest(@Path("id") id: Int): ApiResponse<JsonElement>

    @POST(ApiRoutes.TOGGLE_NOTIFICATION)
    suspend fun toggleNotification(
        @Body body: ToggleNotificationRequest,
    ): ApiResponse<JsonElement>
}

interface PagesApi {

    @GET(ApiRoutes.STATIC_CONTENT)
    suspend fun getPages(): ApiResponse<List<StaticPageDto>>

    @GET("${ApiRoutes.STATIC_CONTENT}/{slug}")
    suspend fun getPageBySlug(@Path("slug") slug: String): ApiResponse<StaticPageDto>
}

interface PatientProfileApi {

    @POST(ApiRoutes.PATIENT_PROFILE_COMPLETE)
    suspend fun completeProfile(@Body body: JsonElement): ApiResponse<JsonElement>

    @GET("${'$'}{ApiRoutes.PATIENT_REVIEW}{code}")
    suspend fun getPatientReview(@Path("code") code: String): ApiResponse<PatientReviewDto>

    @GET(ApiRoutes.PATIENT_PROFILE)
    suspend fun getPrefill(): ApiResponse<PatientPrefillDto>
}

interface MiscApi {

    @POST(ApiRoutes.BOOKKEEPER_INVOICE)
    suspend fun createInvoice(@Body body: InvoiceRequest): ApiResponse<JsonElement>

    @POST(ApiRoutes.USER_UNAVAILABILITY)
    suspend fun setUnavailability(@Body body: UnavailabilityRequest): ApiResponse<JsonElement>
}
