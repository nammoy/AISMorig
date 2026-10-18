package com.app.aisheildme.bytrix.data.remote.api

import com.app.aisheildme.bytrix.data.remote.ApiRoutes
import com.app.aisheildme.bytrix.data.remote.dto.ApiResponse
import com.app.aisheildme.bytrix.data.remote.dto.ChangePasswordRequest
import com.app.aisheildme.bytrix.data.remote.dto.ForgotTokenDto
import com.app.aisheildme.bytrix.data.remote.dto.LoginRequest
import com.app.aisheildme.bytrix.data.remote.dto.RegisterRequest
import com.app.aisheildme.bytrix.data.remote.dto.ResetPinRequest
import com.app.aisheildme.bytrix.data.remote.dto.SendOtpRequest
import com.app.aisheildme.bytrix.data.remote.dto.SetPasswordRequest
import com.app.aisheildme.bytrix.data.remote.dto.SetPinRequest
import com.app.aisheildme.bytrix.data.remote.dto.UpdateProfileRequest
import com.app.aisheildme.bytrix.data.remote.dto.UserDto
import com.app.aisheildme.bytrix.data.remote.dto.VerifyOtpRequest
import com.app.aisheildme.bytrix.data.remote.dto.VerifyPinRequest
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApi {

    @POST(ApiRoutes.LOGIN)
    suspend fun login(@Body body: LoginRequest): ApiResponse<UserDto>

    @POST(ApiRoutes.USER)
    suspend fun register(@Body body: RegisterRequest): ApiResponse<JsonElement>

    @POST(ApiRoutes.VERIFY_OTP_REGISTER)
    suspend fun verifyOtpRegister(@Body body: VerifyOtpRequest): ApiResponse<UserDto>

    @POST(ApiRoutes.SEND_OTP)
    suspend fun sendOtp(@Body body: SendOtpRequest): ApiResponse<JsonElement>

    @POST(ApiRoutes.VERIFY_OTP_FORGOT_PASSWORD)
    suspend fun verifyOtpForgot(@Body body: VerifyOtpRequest): ApiResponse<ForgotTokenDto>

    @POST(ApiRoutes.SET_NEW_PASSWORD)
    suspend fun setNewPassword(
        @Header("Authorization") token: String,
        @Body body: SetPasswordRequest,
    ): ApiResponse<JsonElement>

    @POST(ApiRoutes.CHANGE_PASSWORD)
    suspend fun changePassword(@Body body: ChangePasswordRequest): ApiResponse<JsonElement>

    @GET(ApiRoutes.USER)
    suspend fun getProfile(): ApiResponse<UserDto>

    @PATCH(ApiRoutes.USER)
    suspend fun updateProfile(@Body body: UpdateProfileRequest): ApiResponse<UserDto>

    @PATCH(ApiRoutes.USER)
    suspend fun patchProfile(@Body body: JsonElement): ApiResponse<UserDto>

    @POST(ApiRoutes.SET_PIN)
    suspend fun setPin(@Body body: SetPinRequest): ApiResponse<JsonElement>

    @POST(ApiRoutes.VERIFY_PIN)
    suspend fun verifyPin(@Body body: VerifyPinRequest): ApiResponse<JsonElement>

    @POST(ApiRoutes.RESET_PIN)
    suspend fun resetPin(@Body body: ResetPinRequest): ApiResponse<JsonElement>

    @POST(ApiRoutes.LOGOUT)
    suspend fun logout(): ApiResponse<JsonElement>

    @DELETE(ApiRoutes.USER)
    suspend fun deleteAccount(): ApiResponse<JsonElement>
}
