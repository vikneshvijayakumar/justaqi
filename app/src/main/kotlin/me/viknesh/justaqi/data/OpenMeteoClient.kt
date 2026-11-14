package me.viknesh.justaqi.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.viknesh.justaqi.core.CpcbAqi
import me.viknesh.justaqi.core.Pollutant
import me.viknesh.justaqi.core.Sunscreen
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Fetches modelled air quality and UV from Open-Meteo (CAMS data) and turns it into an [AirSnapshot].
 * Data by Open-Meteo.com, CC BY 4.0. Free for non-commercial use; check their terms before adding ads.
 */
object OpenMeteoClient {
    private const val BASE = "https://air-quality-api.open-meteo.com/v1/air-quality"
    private val IST: ZoneId = ZoneId.of("Asia/Kolkata")
    private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    suspend fun fetch(place: Place, label: String): AirSnapshot = withContext(Dispatchers.IO) {
        val url = URL(
            "$BASE?latitude=${place.lat}&longitude=${place.lon}" +
                "&hourly=pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,us_aqi,uv_index" +
                "&past_days=2&forecast_days=1&timezone=Asia%2FKolkata",
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "justaqi-android/0.1 (open source)")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Open-Meteo returned HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parse(body, label, ZonedDateTime.now(IST))
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(body: String, label: String, now: ZonedDateTime): AirSnapshot {
        val hourly = JSONObject(body).getJSONObject("hourly")

        val timesJson = hourly.getJSONArray("time")
        val times = List(timesJson.length()) { timesJson.getString(it) }
        val nowHour = now.withZoneSameInstant(IST).truncatedTo(ChronoUnit.HOURS)
        val index = times.indexOf(nowHour.format(HOUR_FORMAT))
        if (index < 0) throw IOException("Current hour missing from Open-Meteo response")

        fun series(key: String): List<Double?> {
            val arr = hourly.optJSONArray(key) ?: return emptyList()
            return List(arr.length()) { if (arr.isNull(it)) null else arr.getDouble(it) }
        }

        val pm25 = series("pm2_5")
        val cpcb = CpcbAqi.compute(
            mapOf(
                Pollutant.PM25 to pm25,
                Pollutant.PM10 to series("pm10"),
                Pollutant.NO2 to series("nitrogen_dioxide"),
                Pollutant.SO2 to series("sulphur_dioxide"),
                Pollutant.O3 to series("ozone"),
                Pollutant.CO to series("carbon_monoxide"),
            ),
            index,
        )

        val uv = series("uv_index")
        val today = nowHour.toLocalDate().toString()
        val todayIndexes = times.indices.filter { times[it].startsWith(today) }
        val todayHours = todayIndexes.map { times[it].substring(11, 13).toInt() }
        val todayUv = todayIndexes.map { uv.getOrNull(it) }
        val window = Sunscreen.protectionWindow(todayHours, todayUv)

        return AirSnapshot(
            placeLabel = label,
            fetchedAtMs = System.currentTimeMillis(),
            cpcbAqi = cpcb?.aqi,
            cpcbDominant = cpcb?.dominant,
            usAqi = series("us_aqi").getOrNull(index)?.roundToInt(),
            pm25 = pm25.getOrNull(index),
            uvNow = uv.getOrNull(index),
            uvMax = todayUv.filterNotNull().maxOrNull(),
            uvFromHour = window?.first,
            uvToHour = window?.last,
        )
    }
}
