package com.app.aisheildme.bytrix.tier4_vendor.api

import com.app.aisheildme.bytrix.tier2_vault.entities.VendorConnectionEntity
import com.app.aisheildme.bytrix.tier2_vault.model.Channel
import com.app.aisheildme.bytrix.tier2_vault.model.ServiceType
import com.app.aisheildme.bytrix.tier4_vendor.ConnectorResult
import com.app.aisheildme.bytrix.tier4_vendor.OutboundRequest
import com.app.aisheildme.bytrix.tier4_vendor.VendorConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.CertificatePinner
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simulated vendors (scheme `mock://`) used for development and the end-to-end tests.
 * Behaves like a well-behaved vendor API: honours idempotency keys, returns deterministic codes,
 * and confirms some operations asynchronously.
 */
@Singleton
class MockVendorConnector @Inject constructor() : VendorConnector {
    override val channel = Channel.REST_API
    override val requiresNetwork = false

    private val seen = ConcurrentHashMap<String, ConnectorResult>()

    override fun supports(connection: VendorConnectionEntity?) =
        connection?.endpointUrl?.startsWith("mock://") == true

    override suspend fun submit(request: OutboundRequest): ConnectorResult =
        seen.getOrPut(request.idempotencyKey) { respond(request) }

    override suspend fun status(request: OutboundRequest, reference: String?): ConnectorResult? {
        val prior = seen[request.idempotencyKey] ?: return null
        return when (prior) {
            is ConnectorResult.Accepted -> ConnectorResult.Confirmed(prior.reference, "${request.vendorName} confirmed request ${prior.reference}.")
            else -> prior
        }
    }

    private fun respond(request: OutboundRequest): ConnectorResult {
        val code = shortCode(request.idempotencyKey)
        val missing = required(request.service).filter { request.payload[it].isNullOrBlank() }
        if (missing.isNotEmpty()) return ConnectorResult.Rejected("Missing required field(s): ${missing.joinToString()}", 422)
        return when (request.service) {
            ServiceType.APPOINTMENT_CHECK_IN -> ConnectorResult.Confirmed("CHK-$code", "${request.vendorName}: you're checked in.")
            ServiceType.RETURN -> ConnectorResult.Accepted("RMA-$code", "${request.vendorName} received your return request.")
            ServiceType.CHARGE_DISPUTE -> ConnectorResult.Accepted("DSP-$code", "${request.vendorName} opened a dispute case.")
            ServiceType.MESSAGING -> ConnectorResult.Confirmed("MSG-$code", "Message delivered to ${request.vendorName}.")
            else -> ConnectorResult.Confirmed("REQ-$code", "${request.vendorName} accepted the request.")
        }
    }

    companion object {
        fun required(service: ServiceType): List<String> = when (service) {
            ServiceType.APPOINTMENT_CHECK_IN -> listOf("first_name", "last_name", "date_of_birth")
            ServiceType.RETURN -> listOf("order_number")
            ServiceType.CHARGE_DISPUTE -> listOf("transaction_amount", "transaction_date", "account_last4")
            else -> emptyList()
        }

        fun shortCode(key: String): String =
            MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
                .take(4).joinToString("") { "%02X".format(it) }
    }
}

/**
 * Authorized vendor REST/FHIR APIs over HTTPS only, with optional certificate pinning, an
 * `Idempotency-Key` header and a bearer credential looked up by alias (never stored in Room).
 * No real vendor endpoints ship with this build; vendors are added through TrustedVendorRegistry.
 */
@Singleton
class AuthorizedApiConnector @Inject constructor(
    private val credentials: VendorCredentialStore,
) : VendorConnector {
    override val channel = Channel.REST_API
    private val json = Json { ignoreUnknownKeys = true }

    override fun supports(connection: VendorConnectionEntity?) =
        connection?.endpointUrl?.startsWith("https://") == true &&
            (connection.authType == "BEARER" || connection.authType == "NONE" || connection.authType == "FHIR_SMART")

    override suspend fun submit(request: OutboundRequest): ConnectorResult = withContext(Dispatchers.IO) {
        val connection = request.connection ?: return@withContext ConnectorResult.Rejected("No connection configured")
        val url = connection.endpointUrl.toHttpUrlOrNull() ?: return@withContext ConnectorResult.Rejected("Invalid endpoint")
        val body = if (connection.authType == "FHIR_SMART") {
            FhirMapper.bundleFor(request).toString()
        } else {
            JsonObject(request.payload.mapValues { JsonPrimitive(it.value) } + mapOf("purpose" to JsonPrimitive(request.purpose.name))).toString()
        }
        val builder = Request.Builder()
            .url(url)
            .header("Idempotency-Key", request.idempotencyKey)
            .header("X-Request-Id", request.requestUuid)
            .post(body.toRequestBody((if (connection.authType == "FHIR_SMART") "application/fhir+json" else "application/json").toMediaType()))
        connection.credentialAlias?.let { alias ->
            val token = credentials.token(alias) ?: return@withContext ConnectorResult.Rejected("Vendor sign-in required")
            builder.header("Authorization", "Bearer $token")
        }
        try {
            client(connection, url.host).newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty().take(4_000)
                val obj = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
                val code = obj?.get("confirmation")?.jsonPrimitive?.content ?: obj?.get("id")?.jsonPrimitive?.content
                when {
                    response.code in 200..201 && code != null -> ConnectorResult.Confirmed(code, "Confirmed by ${request.vendorName}.")
                    response.code in 200..202 -> ConnectorResult.Accepted(code ?: request.requestUuid, "Accepted by ${request.vendorName}.")
                    response.code == 409 -> ConnectorResult.Accepted(code ?: request.requestUuid, "Already received by ${request.vendorName}.")
                    response.code in 500..599 -> ConnectorResult.NetworkFailure("Vendor error ${response.code}", mayHaveReachedVendor = true)
                    else -> ConnectorResult.Rejected("Vendor declined (${response.code})", response.code)
                }
            }
        } catch (e: UnknownHostException) {
            ConnectorResult.NetworkFailure("No connection", mayHaveReachedVendor = false)
        } catch (e: SocketTimeoutException) {
            ConnectorResult.NetworkFailure("Timed out", mayHaveReachedVendor = true)
        } catch (e: IOException) {
            ConnectorResult.NetworkFailure(e.message ?: "Network error", mayHaveReachedVendor = true)
        }
    }

    private fun client(connection: VendorConnectionEntity, host: String): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false)
        connection.certificatePin?.let { pin ->
            builder.certificatePinner(CertificatePinner.Builder().add(host, "sha256/$pin").build())
        }
        return builder.build()
    }
}

/** Fallback when no authorized automated channel exists: guide the user through the official process. */
@Singleton
class ManualProcessConnector @Inject constructor() : VendorConnector {
    override val channel = Channel.MANUAL
    override val requiresNetwork = false

    override fun supports(connection: VendorConnectionEntity?) = true

    override suspend fun submit(request: OutboundRequest): ConnectorResult {
        val destination = request.payload["dispute_channel"]?.takeIf { it.isNotBlank() }
            ?: request.connection?.endpointUrl?.takeIf { it.startsWith("https://") }
            ?: "${request.vendorName}'s official website, app or phone line"
        val steps = when (request.service) {
            ServiceType.CHARGE_DISPUTE -> listOf(
                "Open $destination (use the app or the number on your card — not a link from a message).",
                "Choose \"Dispute a transaction\" and select the ${request.payload["transaction_amount"] ?: ""} charge on ${request.payload["transaction_date"] ?: "the date shown"}.",
                "Use the prepared reason: ${request.payload["reason"] ?: "unrecognized charge"}.",
                "Enter the case number here when you receive it so AI-ShieldMe can record it.",
            )
            ServiceType.RETURN -> listOf(
                "Open $destination and find order ${request.payload["order_number"] ?: ""}.",
                "Start a return and give the reason: ${request.payload["reason"] ?: "return"}.",
                "Enter the return authorization number here when you get it.",
            )
            else -> listOf(
                "Contact ${request.vendorName} through $destination.",
                "Share only the details shown on the approval screen.",
                "Enter the confirmation number here when you receive it.",
            )
        }
        return ConnectorResult.ManualRequired(destination, steps)
    }
}

/** Vendor credentials (OAuth tokens etc.) sealed with Keystore, referenced by alias from Room. */
@Singleton
class VendorCredentialStore @Inject constructor(
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) {
    private val prefs get() = context.getSharedPreferences("twentyclaude_vendor_credentials", android.content.Context.MODE_PRIVATE)
    private val cipher by lazy { com.app.aisheildme.bytrix.tier2_vault.security.KeystoreCipher("twentyclaude.vendor.credentials") }

    fun token(alias: String): String? = prefs.getString(alias, null)?.let { sealed ->
        runCatching { String(cipher.decrypt(sealed), Charsets.UTF_8) }.getOrNull()
    }

    fun store(alias: String, token: String) {
        prefs.edit().putString(alias, cipher.encrypt(token.toByteArray(Charsets.UTF_8))).apply()
    }
}
