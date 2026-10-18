package com.app.aisheildme.bytrix.data.remote.dto

import com.app.aisheildme.bytrix.data.remote.FlexibleIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int = -1,
    val message: String = "",
    val data: T? = null,
    val links: Links? = null,
)

@Serializable
data class Links(
    @Serializable(with = FlexibleIntSerializer::class) val first: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class) val last: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class) val prev: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class) val next: Int = 1,
    @Serializable(with = FlexibleIntSerializer::class) val total: Int = 1,
    @SerialName("per_page")
    @Serializable(with = FlexibleIntSerializer::class) val perPage: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class) val current: Int = 1,
    @SerialName("total_records")
    @Serializable(with = FlexibleIntSerializer::class) val totalRecords: Int = 0,
)

@Serializable
data class ApiErrorBody(
    val code: Int = -1,
    val message: String = "",
    val data: kotlinx.serialization.json.JsonElement? = null,
)
