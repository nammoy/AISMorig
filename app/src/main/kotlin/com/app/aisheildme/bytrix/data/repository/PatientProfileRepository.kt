package com.app.aisheildme.bytrix.data.repository

import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.data.remote.api.PatientProfileApi
import com.app.aisheildme.bytrix.data.remote.dto.toDomain
import com.app.aisheildme.bytrix.domain.model.CheckInForm
import com.app.aisheildme.bytrix.domain.model.PatientPrefill
import com.app.aisheildme.bytrix.domain.model.PatientProfilePayload
import com.app.aisheildme.bytrix.domain.model.PatientReview
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientProfileRepository @Inject constructor(
    private val api: PatientProfileApi,
    private val json: Json,
) {

    suspend fun completeProfile(form: CheckInForm): String? {
        val payload = PatientProfilePayload.from(form).toJsonElement()
        val response = api.completeProfile(payload)
        return response.data?.patientCode()
    }

    suspend fun getPatientReview(patientCode: String): PatientReview =
        api.getPatientReview(patientCode).data?.toDomain()
            ?: throw AppException("Could not read the patient review.")

    suspend fun getPrefill(): PatientPrefill =
        api.getPrefill().data?.toDomain() ?: PatientPrefill.EMPTY

    private fun JsonElement.patientCode(): String? {
        val obj = this as? JsonObject ?: return null
        (obj["patient_code"] as? JsonPrimitive)?.content
            ?.takeIf { it.isNotBlank() }?.let { return it }
        val patient = obj["patient"] as? JsonObject ?: return null
        return (patient["patient_code"] as? JsonPrimitive)?.content
            ?.takeIf { it.isNotBlank() }
    }

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> kotlinx.serialization.json.JsonNull
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Map<*, *> -> buildJsonObject {
            forEach { (key, value) -> put(key.toString(), value.toJsonElement()) }
        }
        is Iterable<*> -> buildJsonArray {
            forEach { add(it.toJsonElement()) }
        }
        else -> JsonPrimitive(toString())
    }
}
