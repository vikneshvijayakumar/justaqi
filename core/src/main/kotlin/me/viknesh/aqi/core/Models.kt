package me.viknesh.aqi.core

enum class Pollutant(val label: String) {
    PM25("PM2.5"),
    PM10("PM10"),
    NO2("NO₂"),
    SO2("SO₂"),
    O3("O₃"),
    CO("CO"),
}

enum class AqiScale { CPCB, US }

/** India's National AQI categories (CPCB, 2014). */
enum class AqiCategory(val label: String, val max: Int) {
    GOOD("Good", 50),
    SATISFACTORY("Satisfactory", 100),
    MODERATE("Moderate", 200),
    POOR("Poor", 300),
    VERY_POOR("Very poor", 400),
    SEVERE("Severe", 500);

    companion object {
        fun of(aqi: Int): AqiCategory = entries.firstOrNull { aqi <= it.max } ?: SEVERE
    }
}

/** US EPA AQI categories. [shortLabel] is for tight spaces such as the widget. */
enum class UsAqiCategory(val label: String, val shortLabel: String, val max: Int) {
    GOOD("Good", "Good", 50),
    MODERATE("Moderate", "Moderate", 100),
    UNHEALTHY_SENSITIVE("Unhealthy for sensitive groups", "Sensitive groups", 150),
    UNHEALTHY("Unhealthy", "Unhealthy", 200),
    VERY_UNHEALTHY("Very unhealthy", "Very unhealthy", 300),
    HAZARDOUS("Hazardous", "Hazardous", 500);

    companion object {
        fun of(aqi: Int): UsAqiCategory = entries.firstOrNull { aqi <= it.max } ?: HAZARDOUS
    }
}

data class AqiResult(
    val aqi: Int,
    val category: AqiCategory,
    val dominant: Pollutant,
    val subIndices: Map<Pollutant, Int>,
)
