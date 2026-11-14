package me.viknesh.justaqi.ui

import java.util.Locale
import kotlin.math.roundToInt

fun agoText(thenMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val minutes = (nowMs - thenMs) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        else -> "${minutes / (24 * 60)} d ago"
    }
}

/** 0 -> "12 am", 9 -> "9 am", 15 -> "3 pm", 24 -> "12 am". */
fun hourLabel(hour: Int): String {
    val h = hour % 24
    val suffix = if (h < 12) "am" else "pm"
    val display = if (h % 12 == 0) 12 else h % 12
    return "$display $suffix"
}

/** 4.26 -> "4.3", 151.7 -> "152". */
fun concText(value: Double): String =
    if (value < 10) String.format(Locale.US, "%.1f", value) else value.roundToInt().toString()
