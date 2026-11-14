package me.viknesh.justaqi.core

/** WHO UV index bands. */
enum class UvLevel(val label: String, val from: Double) {
    LOW("Low", 0.0),
    MODERATE("Moderate", 3.0),
    HIGH("High", 6.0),
    VERY_HIGH("Very high", 8.0),
    EXTREME("Extreme", 11.0);

    companion object {
        fun of(uv: Double): UvLevel = entries.lastOrNull { uv >= it.from } ?: LOW
    }
}

data class SunscreenAdvice(val level: UvLevel, val headline: String, val detail: String)

/**
 * General sun-protection guidance based on the UV index bands (WHO / SunSmart style).
 * It is not medical advice and does not account for skin type, medication or altitude.
 */
object Sunscreen {

    fun advise(uv: Double): SunscreenAdvice {
        val level = UvLevel.of(uv)
        return when (level) {
            UvLevel.LOW -> SunscreenAdvice(
                level,
                "Sunscreen not needed for most people",
                "UV is low. If you will be outside for hours or have very sensitive skin, a light SPF 30 is fine.",
            )
            UvLevel.MODERATE -> SunscreenAdvice(
                level,
                "Wear SPF 30 if you will be outside",
                "Reapply every 2 hours in direct sun, and after sweating or swimming.",
            )
            UvLevel.HIGH -> SunscreenAdvice(
                level,
                "Wear SPF 30+ and cover up",
                "Reapply every 2 hours. Seek shade around midday. A hat and sunglasses help.",
            )
            UvLevel.VERY_HIGH -> SunscreenAdvice(
                level,
                "Wear SPF 50 and limit midday sun",
                "Reapply every 2 hours. Stay in the shade when you can, and cover up.",
            )
            UvLevel.EXTREME -> SunscreenAdvice(
                level,
                "Avoid midday sun. SPF 50 and cover up",
                "Skin can burn in minutes. Reapply every 2 hours and stay in the shade.",
            )
        }
    }

    /**
     * First to last hour of the day with UV at or above [threshold] (default 3, where
     * protection is recommended), or null if UV stays below it. [hours] are 0-23 and
     * [uv] is aligned to them.
     */
    fun protectionWindow(hours: List<Int>, uv: List<Double?>, threshold: Double = 3.0): IntRange? {
        val hits = hours.indices.filter { (uv.getOrNull(it) ?: 0.0) >= threshold }
        if (hits.isEmpty()) return null
        return hours[hits.first()]..hours[hits.last()]
    }
}
