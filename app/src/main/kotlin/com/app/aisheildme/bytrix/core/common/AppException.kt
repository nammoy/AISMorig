package com.app.aisheildme.bytrix.core.common

class AppException(
    override val message: String,
    val code: Int = -1,
    val data: Any? = null,
) : java.io.IOException(message) {

    companion object {
        const val EMAIL_NOT_VERIFIED = 428

        const val GENERIC = "Something went wrong. Please try again."
        const val NO_INTERNET = "No internet connection."
        const val TIMEOUT = "The connection timed out. Please try again."
    }
}

suspend inline fun <T> runCatchingApp(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: AppException) {
        Result.failure(e)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(AppException(e.message ?: AppException.GENERIC))
    }

fun AppException.emailFromData(): String? {
    val element = data as? kotlinx.serialization.json.JsonElement ?: return null
    val obj = element as? kotlinx.serialization.json.JsonObject ?: return null
    val email = obj["email"] as? kotlinx.serialization.json.JsonPrimitive ?: return null
    return email.content.takeIf { it.isNotBlank() }
}
