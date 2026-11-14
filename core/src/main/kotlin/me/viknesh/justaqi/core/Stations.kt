package me.viknesh.justaqi.core

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * r * asin(sqrt(a))
}

/**
 * One CPCB monitoring station as published on data.gov.in: a 24 hour average sub-index
 * per pollutant, and when the station last reported.
 */
data class Station(
    val name: String,
    val lat: Double,
    val lon: Double,
    val updatedAtMs: Long?,
    val subIndices: Map<Pollutant, Int>,
) {
    /** CPCB rule: at least 3 pollutants, one of them PM2.5 or PM10. Null when the station falls short. */
    val dominant: Pollutant?
        get() {
            val hasPm = Pollutant.PM25 in subIndices || Pollutant.PM10 in subIndices
            if (subIndices.size < 3 || !hasPm) return null
            return subIndices.maxByOrNull { it.value }?.key
        }

    val aqi: Int? get() = dominant?.let { subIndices.getValue(it) }
}

data class StationPick(val station: Station, val km: Double)

/**
 * Chooses which station speaks for a location. Freshness beats distance:
 *  1. the nearest station within [MAX_KM] that reported in the last [FRESH_MS];
 *  2. else the nearest within [MAX_KM] that reported in the last [USABLE_MS] (shown as old);
 *  3. else none, and the caller falls back to the modelled estimate.
 * So a station 10 km away with a current reading wins over one 5 km away that went quiet.
 * The three limits are judgment calls, not CPCB rules. Tune them here.
 */
object StationPicker {
    const val MAX_KM = 25.0
    const val FRESH_MS = 3 * 60 * 60 * 1000L
    const val USABLE_MS = 24 * 60 * 60 * 1000L

    fun pick(stations: List<Station>, lat: Double, lon: Double, nowMs: Long): StationPick? {
        val inRange = stations
            .filter { it.aqi != null && it.updatedAtMs != null }
            .map { StationPick(it, distanceKm(lat, lon, it.lat, it.lon)) }
            .filter { it.km <= MAX_KM }
            .sortedBy { it.km }
        fun age(p: StationPick) = nowMs - p.station.updatedAtMs!!
        return inRange.firstOrNull { age(it) <= FRESH_MS }
            ?: inRange.firstOrNull { age(it) <= USABLE_MS }
    }
}

/** US EPA AQI for PM2.5 (24 hour, 2024 breakpoints). Bands treated as contiguous, like [CpcbAqi]. */
object UsAqi {
    private val CONC_UPPER = doubleArrayOf(9.0, 35.4, 55.4, 125.4, 225.4, 325.4)
    private val AQI_UPPER = intArrayOf(50, 100, 150, 200, 300, 500)

    fun pm25(concentration: Double): Int {
        var cLow = 0.0
        var iLow = 0
        for (k in CONC_UPPER.indices) {
            if (concentration <= CONC_UPPER[k] || k == CONC_UPPER.lastIndex) {
                val fraction = ((concentration - cLow) / (CONC_UPPER[k] - cLow)).coerceIn(0.0, 1.0)
                return (iLow + fraction * (AQI_UPPER[k] - iLow)).roundToInt()
            }
            cLow = CONC_UPPER[k]
            iLow = AQI_UPPER[k]
        }
        error("unreachable")
    }
}
