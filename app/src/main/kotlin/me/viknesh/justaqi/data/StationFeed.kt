package me.viknesh.justaqi.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.viknesh.justaqi.core.Pollutant
import me.viknesh.justaqi.core.Station
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Every CPCB station in India, as one small JSON file. A scheduled GitHub workflow
 * (.github/workflows/feed.yml) copies it from data.gov.in to the repo's `data` branch, so the
 * app needs no API key and data.gov.in sees one request per run instead of one per phone.
 * ponytail: a static file on GitHub. Move to a real cache (Cloudflare Worker) if the schedule
 * proves too slow or users grow past what raw.githubusercontent.com should serve.
 */
object StationFeed {
    private const val URL_FEED = "https://raw.githubusercontent.com/vikneshvijayakumar/justaqi/data/stations.json"
    private val IST: ZoneId = ZoneId.of("Asia/Kolkata")
    private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")

    private val IDS = mapOf(
        "PM2.5" to Pollutant.PM25,
        "PM10" to Pollutant.PM10,
        "NO2" to Pollutant.NO2,
        "SO2" to Pollutant.SO2,
        "CO" to Pollutant.CO,
        "OZONE" to Pollutant.O3,
        "NH3" to Pollutant.NH3,
    )

    suspend fun fetch(): List<Station> = withContext(Dispatchers.IO) {
        val connection = (URL(URL_FEED).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Station feed returned HTTP ${connection.responseCode}")
            }
            parse(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    /** Skips stations with unusable coordinates. Missing ("NA") pollutants are simply absent. */
    internal fun parse(body: String): List<Station> {
        val arr = JSONObject(body).getJSONArray("stations")
        return (0 until arr.length()).mapNotNull { i ->
            val s = arr.getJSONObject(i)
            val lat = s.optDouble("lat")
            val lon = s.optDouble("lon")
            if (lat.isNaN() || lon.isNaN()) return@mapNotNull null
            val p = s.optJSONObject("p") ?: JSONObject()
            val subs = LinkedHashMap<Pollutant, Int>()
            for ((id, pollutant) in IDS) {
                if (p.has(id) && !p.isNull(id)) subs[pollutant] = p.getInt(id)
            }
            val updated = runCatching {
                LocalDateTime.parse(s.getString("t"), STAMP).atZone(IST).toInstant().toEpochMilli()
            }.getOrNull()
            Station(s.getString("n"), lat, lon, updated, subs)
        }
    }
}
