package com.app.aisheildme.bytrix.core.common

import java.util.UUID

data class UiMessage(
    val text: String,
    val type: Type = Type.Error,
    val id: String = UUID.randomUUID().toString(),
) {
    enum class Type { Success, Error }

    companion object {
        fun success(text: String) = UiMessage(text, Type.Success)
        fun error(text: String) = UiMessage(text, Type.Error)
    }
}
