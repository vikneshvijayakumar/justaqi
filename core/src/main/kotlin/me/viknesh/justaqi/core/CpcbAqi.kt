package me.viknesh.justaqi.core

import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * India's National AQI (CPCB, 2014), computed from hourly pollutant series.
 *
 * Method, per CPCB:
 *  - Each pollutant gets a sub-index by linear interpolation within its concentration band.
 *  - PM2.5, PM10, NO2, SO2 use a 24-hour mean; O3 and CO use an 8-hour mean.
 *  - The overall AQI is the worst (highest) sub-index.
 *  - An AQI is only valid with at least 3 pollutants, one of them PM2.5 or PM10.
 *  - A pollutant needs at least two thirds of the hours in its window (16 of 24, 6 of 8).
 *
 * Not included: NH3 and Pb (no hourly source for India in our data feed).
 *
 * Inputs are in µg/m³ (the unit Open-Meteo uses for every pollutant, CO included).
 * CO is converted to mg/m³ here because the CPCB table is in mg/m³.
 */
object CpcbAqi {

    private val AQI_UPPER = intArrayOf(50, 100, 200, 300, 400, 500)

    /**
     * Upper concentration bound of each band: Good, Satisfactory, Moderate, Poor, Very poor, Severe.
     * Units: µg/m³, except CO in mg/m³. Values verified against the CPCB table
     * (https://ies.gov.in/arthapedia/concept/national-air-quality-index).
     *
     * CPCB leaves "Severe" open-ended (e.g. "250+"). ASSUMPTION: we cap the Severe band at
     * the Very poor upper bound plus the Very poor band width, and clamp the AQI at 500.
     * That is not an official value; see docs/DECISIONS.md.
     */
    private val CONC_UPPER: Map<Pollutant, DoubleArray> = mapOf(
        Pollutant.PM25 to doubleArrayOf(30.0, 60.0, 90.0, 120.0, 250.0, 380.0),
        Pollutant.PM10 to doubleArrayOf(50.0, 100.0, 250.0, 350.0, 430.0, 510.0),
        Pollutant.NO2 to doubleArrayOf(40.0, 80.0, 180.0, 280.0, 400.0, 520.0),
        Pollutant.SO2 to doubleArrayOf(40.0, 80.0, 380.0, 800.0, 1600.0, 2400.0),
        Pollutant.O3 to doubleArrayOf(50.0, 100.0, 168.0, 208.0, 748.0, 1288.0),
        Pollutant.CO to doubleArrayOf(1.0, 2.0, 10.0, 17.0, 34.0, 51.0),
    )

    fun windowHours(p: Pollutant): Int = if (p == Pollutant.O3 || p == Pollutant.CO) 8 else 24

    /**
     * Sub-index for one pollutant. [concentration] is in µg/m³ (mg/m³ for CO).
     * Bands are treated as contiguous, so 30.5 µg/m³ of PM2.5 sits between the
     * Good and Satisfactory rows instead of falling through a gap.
     */
    fun subIndex(pollutant: Pollutant, concentration: Double): Int {
        val upper = CONC_UPPER.getValue(pollutant)
        var cLow = 0.0
        var iLow = 0
        for (k in upper.indices) {
            val cHigh = upper[k]
            val iHigh = AQI_UPPER[k]
            if (concentration <= cHigh || k == upper.lastIndex) {
                val fraction = ((concentration - cLow) / (cHigh - cLow)).coerceIn(0.0, 1.0)
                return (iLow + fraction * (iHigh - iLow)).roundToInt()
            }
            cLow = cHigh
            iLow = iHigh
        }
        error("unreachable")
    }

    /**
     * Reverse of [subIndex]: the concentration that gives [index]. Used to estimate a
     * concentration from station data, which arrives as sub-indices only.
     */
    fun concentrationFor(pollutant: Pollutant, index: Int): Double {
        val upper = CONC_UPPER.getValue(pollutant)
        var cLow = 0.0
        var iLow = 0
        for (k in upper.indices) {
            if (index <= AQI_UPPER[k] || k == upper.lastIndex) {
                val fraction = ((index - iLow).toDouble() / (AQI_UPPER[k] - iLow)).coerceIn(0.0, 1.0)
                return cLow + fraction * (upper[k] - cLow)
            }
            cLow = upper[k]
            iLow = AQI_UPPER[k]
        }
        error("unreachable")
    }

    /**
     * Mean of the [hours] values ending at [endIndex] (inclusive), or null when fewer than
     * two thirds of them are present.
     */
    fun windowMean(values: List<Double?>, endIndex: Int, hours: Int): Double? {
        if (endIndex < 0 || values.isEmpty()) return null
        val end = minOf(endIndex, values.lastIndex)
        val start = maxOf(0, end - hours + 1)
        var sum = 0.0
        var count = 0
        for (i in start..end) {
            val v = values[i]
            if (v != null && !v.isNaN()) {
                sum += v
                count++
            }
        }
        val needed = ceil(hours * 2.0 / 3.0).toInt()
        return if (count >= needed) sum / count else null
    }

    /**
     * @param series hourly values per pollutant, oldest first, all aligned to the same hours
     * @param endIndex index of the current hour in those series
     */
    fun compute(series: Map<Pollutant, List<Double?>>, endIndex: Int): AqiResult? {
        val subs = LinkedHashMap<Pollutant, Int>()
        val concentrations = LinkedHashMap<Pollutant, Double>()
        for ((pollutant, values) in series) {
            val mean = windowMean(values, endIndex, windowHours(pollutant)) ?: continue
            val concentration = if (pollutant == Pollutant.CO) mean / 1000.0 else mean
            subs[pollutant] = subIndex(pollutant, concentration)
            concentrations[pollutant] = concentration
        }
        val hasParticulate = Pollutant.PM25 in subs || Pollutant.PM10 in subs
        if (subs.size < 3 || !hasParticulate) return null
        val worst = subs.maxByOrNull { it.value } ?: return null
        return AqiResult(
            aqi = worst.value,
            category = AqiCategory.of(worst.value),
            dominant = worst.key,
            subIndices = subs,
            concentrations = concentrations,
        )
    }
}
