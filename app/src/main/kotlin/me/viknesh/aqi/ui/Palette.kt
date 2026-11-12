package me.viknesh.aqi.ui

import androidx.compose.ui.graphics.Color
import me.viknesh.aqi.core.AqiCategory
import me.viknesh.aqi.core.AqiScale
import me.viknesh.aqi.core.UsAqiCategory

data class Band(val bg: Color, val fg: Color)

/**
 * One colour per air-quality band, shared by the app and the widget. Both scales have six bands,
 * so they share a palette. Text and background pairs were picked for at least 4.5:1 contrast;
 * re-check with a contrast tool before shipping. The number and label always appear with the
 * colour, so colour is never the only signal.
 */
object Palette {
    val INK = Color(0xFF1A1A1A)
    val NEUTRAL_BG = Color(0xFFEDEDED)

    private val bands = listOf(
        Band(Color(0xFF2E7D32), Color.White), // Good
        Band(Color(0xFF9CCC3C), INK),         // Satisfactory / Moderate (US)
        Band(Color(0xFFF9D423), INK),         // Moderate / Sensitive groups (US)
        Band(Color(0xFFF57C00), INK),         // Poor / Unhealthy (US)
        Band(Color(0xFFD32F2F), Color.White), // Very poor / Very unhealthy (US)
        Band(Color(0xFF7B1F1F), Color.White), // Severe / Hazardous (US)
    )

    private fun ordinal(scale: AqiScale, aqi: Int): Int =
        if (scale == AqiScale.CPCB) AqiCategory.of(aqi).ordinal else UsAqiCategory.of(aqi).ordinal

    fun band(scale: AqiScale, aqi: Int): Band = bands[ordinal(scale, aqi)]

    fun label(scale: AqiScale, aqi: Int): String =
        if (scale == AqiScale.CPCB) AqiCategory.of(aqi).label else UsAqiCategory.of(aqi).label

    fun shortLabel(scale: AqiScale, aqi: Int): String =
        if (scale == AqiScale.CPCB) AqiCategory.of(aqi).label else UsAqiCategory.of(aqi).shortLabel
}
