package com.app.aisheildme.bytrix.core.common

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetailHandoff @Inject constructor() {

    private val entries = mutableMapOf<String, Any>()

    fun <T : Any> put(key: String, value: T) {
        entries[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String): T? = entries[key] as? T

    fun clear(key: String) {
        entries.remove(key)
    }

    companion object {
        const val RETAIL_ORDER = "retail_order"
        const val BANK_STATEMENT = "bank_statement"
        const val APPOINTMENT = "appointment"
        const val SHARE_DOCUMENT = "share_document"
        const val CHECK_IN_FORM = "check_in_form"
        const val CHECK_IN_REVIEW = "check_in_review"
    }
}
