package me.viknesh.justaqi.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CpcbAqiTest {

    private fun flat(value: Double?, n: Int = 48): List<Double?> = List(n) { value }

    // Worked examples from the CPCB methodology page.
    @Test
    fun pm25_worked_examples() {
        assertEquals(75, CpcbAqi.subIndex(Pollutant.PM25, 45.0))
        assertEquals(323, CpcbAqi.subIndex(Pollutant.PM25, 150.0))
    }

    @Test
    fun pm25_band_edges() {
        assertEquals(0, CpcbAqi.subIndex(Pollutant.PM25, 0.0))
        assertEquals(50, CpcbAqi.subIndex(Pollutant.PM25, 30.0))
        assertEquals(100, CpcbAqi.subIndex(Pollutant.PM25, 60.0))
        assertEquals(200, CpcbAqi.subIndex(Pollutant.PM25, 90.0))
        assertEquals(300, CpcbAqi.subIndex(Pollutant.PM25, 120.0))
        assertEquals(400, CpcbAqi.subIndex(Pollutant.PM25, 250.0))
    }

    @Test
    fun other_pollutants_midband() {
        assertEquals(150, CpcbAqi.subIndex(Pollutant.PM10, 175.0))
        assertEquals(150, CpcbAqi.subIndex(Pollutant.CO, 6.0)) // mg/m³
        assertEquals(50, CpcbAqi.subIndex(Pollutant.CO, 1.0))
        assertEquals(100, CpcbAqi.subIndex(Pollutant.O3, 100.0))
        assertEquals(200, CpcbAqi.subIndex(Pollutant.NO2, 180.0))
    }

    @Test
    fun values_gap_between_rows_is_continuous() {
        val below = CpcbAqi.subIndex(Pollutant.PM25, 30.0)
        val gap = CpcbAqi.subIndex(Pollutant.PM25, 30.5)
        val above = CpcbAqi.subIndex(Pollutant.PM25, 31.0)
        assert(below <= gap && gap <= above) { "$below $gap $above" }
    }

    @Test
    fun aqi_is_capped_at_500() {
        assertEquals(500, CpcbAqi.subIndex(Pollutant.PM25, 1000.0))
    }

    @Test
    fun negative_concentration_is_zero() {
        assertEquals(0, CpcbAqi.subIndex(Pollutant.PM10, -5.0))
    }

    @Test
    fun overall_is_worst_subindex() {
        val result = CpcbAqi.compute(
            mapOf(
                Pollutant.PM25 to flat(150.0),
                Pollutant.PM10 to flat(100.0),
                Pollutant.NO2 to flat(40.0),
            ),
            endIndex = 47,
        )
        assertNotNull(result)
        assertEquals(323, result.aqi)
        assertEquals(Pollutant.PM25, result.dominant)
        assertEquals(AqiCategory.VERY_POOR, result.category)
    }

    @Test
    fun co_is_converted_from_ug_to_mg() {
        val result = CpcbAqi.compute(
            mapOf(
                Pollutant.PM25 to flat(10.0),
                Pollutant.PM10 to flat(10.0),
                Pollutant.CO to flat(6000.0), // 6 mg/m³
            ),
            endIndex = 47,
        )
        assertNotNull(result)
        assertEquals(150, result.subIndices.getValue(Pollutant.CO))
        assertEquals(Pollutant.CO, result.dominant)
    }

    @Test
    fun needs_a_particulate_pollutant() {
        val result = CpcbAqi.compute(
            mapOf(
                Pollutant.NO2 to flat(40.0),
                Pollutant.SO2 to flat(10.0),
                Pollutant.O3 to flat(50.0),
            ),
            endIndex = 47,
        )
        assertNull(result)
    }

    @Test
    fun needs_at_least_three_pollutants() {
        val result = CpcbAqi.compute(
            mapOf(Pollutant.PM25 to flat(50.0), Pollutant.PM10 to flat(80.0)),
            endIndex = 47,
        )
        assertNull(result)
    }

    @Test
    fun window_mean_uses_only_the_last_24_hours() {
        val values = List(24) { 0.0 } + List(24) { 100.0 }
        assertEquals(100.0, CpcbAqi.windowMean(values, 47, 24))
    }

    @Test
    fun window_mean_requires_two_thirds_of_hours() {
        val tooFew = List(48) { i -> if (i >= 38) null else 10.0 } // 14 of last 24 present
        assertNull(CpcbAqi.windowMean(tooFew, 47, 24))
        val enough = List(48) { i -> if (i >= 40 && i < 48) null else 10.0 } // 16 of last 24
        assertEquals(10.0, CpcbAqi.windowMean(enough, 47, 24))
        assertNull(CpcbAqi.windowMean(List(10) { 5.0 }, 9, 24)) // short history
    }

    @Test
    fun category_edges() {
        assertEquals(AqiCategory.GOOD, AqiCategory.of(50))
        assertEquals(AqiCategory.SATISFACTORY, AqiCategory.of(51))
        assertEquals(AqiCategory.MODERATE, AqiCategory.of(200))
        assertEquals(AqiCategory.POOR, AqiCategory.of(201))
        assertEquals(AqiCategory.VERY_POOR, AqiCategory.of(301))
        assertEquals(AqiCategory.SEVERE, AqiCategory.of(401))
        assertEquals(AqiCategory.SEVERE, AqiCategory.of(900))
        assertEquals(UsAqiCategory.UNHEALTHY_SENSITIVE, UsAqiCategory.of(120))
    }
}
