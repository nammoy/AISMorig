package com.app.aisheildme.bytrix.data.remote

import com.app.aisheildme.bytrix.BuildConfig
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.data.local.SessionStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthInterceptor(
    private val sessionStore: SessionStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        if (!original.url.host.equals(API_HOST, ignoreCase = true)) {
            return chain.proceed(original)
        }

        val builder = original.newBuilder()
            .header("token", BuildConfig.APP_TOKEN)
            .header("Accept", "application/json")

        if (original.header("Authorization") == null) {
            val stored = runBlocking { sessionStore.apiToken() }
            if (!stored.isNullOrEmpty()) {
                builder.header("Authorization", stored)
            }
        }

        return chain.proceed(builder.build())
    }

    private companion object {
        val API_HOST: String = BuildConfig.BASE_URL.toHttpUrlOrNull()?.host ?: ""
    }
}

class UnauthorizedInterceptor(
    private val sessionStore: SessionStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            runBlocking { sessionStore.clear() }
        }
        return response
    }
}

class ErrorMappingInterceptor(
    private val json: Json,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = try {
            chain.proceed(chain.request())
        } catch (e: java.net.SocketTimeoutException) {
            throw AppException(AppException.TIMEOUT)
        } catch (e: java.net.UnknownHostException) {
            throw AppException(AppException.NO_INTERNET)
        } catch (e: AppException) {
            throw e
        } catch (e: IOException) {
            throw AppException(e.message ?: AppException.GENERIC)
        }

        if (response.isSuccessful) return response

        val raw = runCatching { response.peekBody(Long.MAX_VALUE).string() }.getOrNull()
        val parsed = raw?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }

        val message = parsed?.get("message")?.jsonPrimitive?.content
            ?: AppException.GENERIC
        val data = parsed?.get("data")

        response.close()
        throw AppException(message = message, code = response.code, data = data)
    }
}
