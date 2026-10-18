package com.app.aisheildme.bytrix.tier4_vendor.api

import com.app.aisheildme.bytrix.tier2_vault.model.ServiceType
import com.app.aisheildme.bytrix.tier4_vendor.OutboundRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.Instant

/**
 * Minimal HL7 FHIR R4 mapping for the data AI-ShieldMe releases. Only fields present in the
 * approved payload are emitted (minimum necessary).
 */
object FhirMapper {

    fun patient(payload: Map<String, String>): JsonObject = buildJsonObject {
        put("resourceType", "Patient")
        if (payload["first_name"] != null || payload["last_name"] != null) {
            putJsonArray("name") {
                add(
                    buildJsonObject {
                        payload["last_name"]?.let { put("family", it) }
                        payload["first_name"]?.let { putJsonArray("given") { add(JsonPrimitive(it)) } }
                    },
                )
            }
        }
        payload["date_of_birth"]?.let { put("birthDate", it) }
        val telecom = buildJsonArray {
            payload["phone"]?.let { add(buildJsonObject { put("system", "phone"); put("value", it) }) }
            payload["email"]?.let { add(buildJsonObject { put("system", "email"); put("value", it) }) }
        }
        if (telecom.isNotEmpty()) put("telecom", telecom)
        payload["address"]?.let { line ->
            putJsonArray("address") {
                add(
                    buildJsonObject {
                        putJsonArray("line") { add(JsonPrimitive(line)) }
                        payload["city"]?.let { put("city", it) }
                        payload["state"]?.let { put("state", it) }
                        payload["postal_code"]?.let { put("postalCode", it) }
                    },
                )
            }
        }
    }

    fun coverage(payload: Map<String, String>): JsonObject? {
        val payer = payload["insurance_payer"] ?: return null
        return buildJsonObject {
            put("resourceType", "Coverage")
            put("status", "active")
            payload["insurance_member_id"]?.let { put("subscriberId", it) }
            putJsonArray("payor") { add(buildJsonObject { put("display", payer) }) }
            payload["insurance_group"]?.let { group ->
                putJsonArray("class") {
                    add(
                        buildJsonObject {
                            putJsonObject("type") { put("text", "group") }
                            put("value", group)
                        },
                    )
                }
            }
        }
    }

    /** Check-in = AppointmentResponse(participantStatus=accepted) + QuestionnaireResponse-style extras. */
    fun bundleFor(request: OutboundRequest): JsonObject = buildJsonObject {
        put("resourceType", "Bundle")
        put("type", "collection")
        put("timestamp", Instant.now().toString())
        putJsonArray("entry") {
            add(buildJsonObject { put("resource", patient(request.payload)) })
            coverage(request.payload)?.let { add(buildJsonObject { put("resource", it) }) }
            if (request.service == ServiceType.APPOINTMENT_CHECK_IN) {
                add(
                    buildJsonObject {
                        put(
                            "resource",
                            buildJsonObject {
                                put("resourceType", "AppointmentResponse")
                                put("participantStatus", "accepted")
                                put("comment", "Checked in via AI-ShieldMe")
                            },
                        )
                    },
                )
            }
            request.payload["medications"]?.split(";")?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { med ->
                add(
                    buildJsonObject {
                        put(
                            "resource",
                            buildJsonObject {
                                put("resourceType", "MedicationStatement")
                                put("status", "active")
                                putJsonObject("medicationCodeableConcept") { put("text", med) }
                            },
                        )
                    },
                )
            }
            request.payload["allergies"]?.let { allergies ->
                add(
                    buildJsonObject {
                        put(
                            "resource",
                            buildJsonObject {
                                put("resourceType", "AllergyIntolerance")
                                putJsonObject("code") { put("text", allergies) }
                            },
                        )
                    },
                )
            }
        }
    }

    fun isBundle(json: JsonObject): Boolean =
        (json["resourceType"] as? JsonPrimitive)?.content == "Bundle" && json["entry"] is JsonArray
}

/** Document exchange formats (spec §8.3 D): JSON, CSV, FHIR JSON. PDF export uses the existing PDF writers. */
object DocumentExchange {

    fun toJson(fields: Map<String, String>): String =
        JsonObject(fields.mapValues { JsonPrimitive(it.value) }).toString()

    fun toCsv(header: List<String>, rows: List<List<String>>): String = buildString {
        appendLine(header.joinToString(",") { escape(it) })
        rows.forEach { row -> appendLine(row.joinToString(",") { escape(it) }) }
    }

    /** RFC 4180 quoting plus a leading quote on cells that a spreadsheet would treat as formulas. */
    fun escape(value: String): String {
        val safe = if (value.startsWith("=") || value.startsWith("+") || value.startsWith("-") || value.startsWith("@")) "'$value" else value
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    /** Parses a simple CSV (bank export). Handles quoted cells. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { cell.append('"'); i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == ',' -> { row.add(cell.toString()); cell.clear() }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(cell.toString()); cell.clear()
                    if (row.any { it.isNotBlank() }) rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); if (row.any { it.isNotBlank() }) rows.add(row) }
        return rows
    }
}
