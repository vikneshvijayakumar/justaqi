package me.viknesh.justaqi.data

import me.viknesh.justaqi.core.AqiScale
import me.viknesh.justaqi.core.Pollutant
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OpenMeteoClientTest {

    private val ist = ZoneId.of("Asia/Kolkata")

    /** Three days (10-12 Nov 2026), hourly, in the shape Open-Meteo returns. */
    private fun fixture(): String {
        val times = (10..12).flatMap { d ->
            (0..23).map { h -> String.format(Locale.US, "2026-11-%02dT%02d:00", d, h) }
        }
        fun arr(f: (Int) -> Double?) = JSONArray(times.indices.map { f(it) ?: JSONObject.NULL })
        val hourly = JSONObject()
            .put("time", JSONArray(times))
            .put("pm2_5", arr { 150.0 })
            .put("pm10", arr { 100.0 })
            .put("nitrogen_dioxide", arr { 40.0 })
            .put("sulphur_dioxide", arr { 10.0 })
            .put("ozone", arr { 50.0 })
            .put("carbon_monoxide", arr { 1000.0 })
            .put("us_aqi", arr { 180.0 })
            .put("uv_index", arr { i -> if (i % 24 in 9..15) 6.0 else 0.0 })
        return JSONObject().put("hourly", hourly).toString()
    }

    @Test
    fun parses_fixture_into_snapshot() {
        val now = ZonedDateTime.of(2026, 11, 12, 7, 30, 0, 0, ist)
        val snap = OpenMeteoClient.parse(fixture(), "Delhi", now)

        assertEquals("Delhi", snap.placeLabel)
        assertEquals(323, snap.cpcbAqi)
        assertEquals(Pollutant.PM25, snap.cpcbDominant)
        assertEquals(180, snap.usAqi)
        assertEquals(150.0, snap.pm25)
        assertEquals(0.0, snap.uvNow)
        assertEquals(6.0, snap.uvMax)
        assertEquals(9, snap.uvFromHour)
        assertEquals(15, snap.uvToHour)
        assertEquals(323, snap.aqiFor(AqiScale.CPCB))
        assertEquals(6, snap.pollutants.size)
        val pm25 = snap.pollutants.first { it.pollutant == Pollutant.PM25 }
        assertEquals(150.0, pm25.concentration)
        assertEquals(323, pm25.subIndex)
        val co = snap.pollutants.first { it.pollutant == Pollutant.CO }
        assertEquals(1.0, co.concentration) // 1000 µg/m³ shown as 1 mg/m³
        assertEquals("mg/m³", co.unit)
        assertEquals(180, snap.aqiFor(AqiScale.US))
    }

    @Test
    fun fails_when_current_hour_is_missing() {
        val now = ZonedDateTime.of(2026, 11, 20, 7, 0, 0, 0, ist)
        assertFailsWith<IOException> { OpenMeteoClient.parse(fixture(), "Delhi", now) }
    }

    @Test
    fun snapshot_round_trips_through_json() {
        val original = AirSnapshot(
            placeLabel = "Near Delhi",
            fetchedAtMs = 1_700_000_000_000L,
            cpcbAqi = 212,
            cpcbDominant = Pollutant.PM10,
            usAqi = null,
            pm25 = 88.5,
            uvNow = 3.0,
            uvMax = 7.0,
            uvFromHour = 9,
            uvToHour = 16,
            pollutants = listOf(PollutantReading(Pollutant.PM10, 262.0, 212)),
        )
        val copy = AirSnapshot.fromJson(original.toJson())
        assertNotNull(copy)
        assertEquals(original, copy)
        assertNull(copy.usAqi)
    }

    @Test
    fun corrupt_json_gives_null() {
        assertNull(AirSnapshot.fromJson("not json"))
    }

    @Test
    fun place_search_parses_results() {
        val body = """{"results":[{"name":"Saket","latitude":28.52,"longitude":77.2,"admin1":"Delhi"},
            {"name":"Delhi","latitude":28.65,"longitude":77.23,"admin1":"Delhi"}]}"""
        val places = PlaceSearch.parse(body)
        assertEquals(listOf("Saket, Delhi", "Delhi"), places.map { it.name })
        assertEquals(28.52, places[0].lat)
        assertEquals(emptyList(), PlaceSearch.parse("{}"))
    }
}
