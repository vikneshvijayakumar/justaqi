package me.viknesh.justaqi.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StationsTest {
    private val now = 1_800_000_000_000L
    private val hour = 60 * 60 * 1000L
    private val full = mapOf(Pollutant.PM25 to 356, Pollutant.PM10 to 257, Pollutant.NO2 to 178)

    // Roughly 5 km and 10 km north of the origin point (0.045 degrees of latitude is about 5 km).
    private fun station(name: String, km5: Int, ageHours: Long?, subs: Map<Pollutant, Int> = full) =
        Station(name, 28.6 + 0.045 * km5, 77.2, ageHours?.let { now - it * hour }, subs)

    private fun pick(vararg s: Station) = StationPicker.pick(s.toList(), 28.6, 77.2, now)?.station?.name

    @Test
    fun aqi_is_worst_sub_index_and_needs_three_pollutants_with_pm() {
        assertEquals(356, station("a", 1, 1).aqi)
        assertEquals(Pollutant.PM25, station("a", 1, 1).dominant)
        assertNull(station("a", 1, 1, mapOf(Pollutant.PM25 to 300, Pollutant.NO2 to 50)).aqi)
        assertNull(station("a", 1, 1, mapOf(Pollutant.NO2 to 1, Pollutant.SO2 to 2, Pollutant.CO to 3)).aqi)
    }

    @Test
    fun nearest_fresh_station_wins() {
        assertEquals("near", pick(station("far", 2, 1), station("near", 1, 1)))
    }

    @Test
    fun fresh_at_10_km_beats_stale_at_5_km() {
        assertEquals("far-fresh", pick(station("near-stale", 1, 8), station("far-fresh", 2, 1)))
    }

    @Test
    fun stale_is_used_when_nothing_fresh_and_dropped_after_a_day() {
        assertEquals("near-stale", pick(station("near-stale", 1, 8), station("far-stale", 2, 6)))
        assertNull(pick(station("dead", 1, 30)))
    }

    @Test
    fun ignores_stations_out_of_range_invalid_or_undated() {
        assertNull(pick(station("too-far", 6, 1)))
        assertNull(pick(station("thin", 1, 1, mapOf(Pollutant.PM25 to 300))))
        assertNull(pick(station("undated", 1, null)))
    }

    @Test
    fun cpcb_inverse_round_trips() {
        assertTrue(abs(CpcbAqi.concentrationFor(Pollutant.PM25, 323) - 150.0) < 0.5)
        assertEquals(60.0, CpcbAqi.concentrationFor(Pollutant.PM25, 100))
        for (c in listOf(10.0, 45.0, 75.0, 110.0, 200.0, 300.0)) {
            val back = CpcbAqi.concentrationFor(Pollutant.PM25, CpcbAqi.subIndex(Pollutant.PM25, c))
            assertTrue(abs(back - c) < 1.0, "$c came back as $back")
        }
    }

    @Test
    fun us_pm25_band_edges() {
        assertEquals(50, UsAqi.pm25(9.0))
        assertEquals(100, UsAqi.pm25(35.4))
        assertEquals(200, UsAqi.pm25(125.4))
        assertEquals(500, UsAqi.pm25(400.0))
    }

    @Test
    fun distance_is_sane() {
        // Delhi to Mumbai is about 1150 km.
        assertTrue(abs(distanceKm(28.6139, 77.2090, 19.0760, 72.8777) - 1150) < 20)
    }
}
