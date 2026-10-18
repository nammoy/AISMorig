package com.app.aisheildme.bytrix.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object TimeFormat {

    private fun fmt(pattern: String) = SimpleDateFormat(pattern, Locale.getDefault())

    fun shortDate(epochMs: Long?): String =
        epochMs?.let { fmt("dd/MM/yy").format(Date(it)) } ?: ""

    fun shortTime(epochMs: Long?): String =
        epochMs?.let { fmt("HH:mm").format(Date(it)) } ?: ""

    fun longDateTime(epochMs: Long?): String =
        epochMs?.let { fmt("dd MMM yyyy 'at' HH:mm").format(Date(it)) } ?: ""

    fun mediumDate(epochMs: Long?): String =
        epochMs?.let { fmt("dd MMMM yyyy").format(Date(it)) } ?: ""

    fun isoDate(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(epochMs))

    fun todayAsPickerMs(): Long {
        val local = java.util.Calendar.getInstance()
        return java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(java.util.Calendar.YEAR),
                local.get(java.util.Calendar.MONTH),
                local.get(java.util.Calendar.DAY_OF_MONTH),
            )
        }.timeInMillis
    }

    fun pickerMediumDate(epochMs: Long?): String =
        epochMs?.let {
            fmt("dd MMMM yyyy").apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(it))
        } ?: ""

    fun isoDateOrEmpty(epochMs: Long?): String = epochMs?.let(::isoDate) ?: ""

    fun timeAgo(epochMs: Long?, now: Long = System.currentTimeMillis()): String {
        if (epochMs == null) return ""
        val diff = (now - epochMs).coerceAtLeast(0)

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            TimeUnit.MILLISECONDS.toSeconds(diff) < 60 -> "just now"
            minutes < 60 -> "$minutes ${plural(minutes, "min")} ago"
            hours < 24 -> "$hours ${plural(hours, "hour")} ago"
            days < 7 -> "$days ${plural(days, "day")} ago"
            else -> {
                val weeks = days / 7
                "$weeks ${plural(weeks, "week")} ago"
            }
        }
    }

    private fun plural(value: Long, unit: String) = if (value == 1L) unit else "${unit}s"
}

object IsoParser {

    private val PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
    )

    fun toEpochMs(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        for (pattern in PATTERNS) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) {
                    parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                return parser.parse(raw)?.time ?: continue
            } catch (_: java.text.ParseException) {
            }
        }
        return null
    }
}
