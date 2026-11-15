package me.viknesh.justaqi.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.viknesh.justaqi.BuildConfig
import me.viknesh.justaqi.core.Pollutant
import me.viknesh.justaqi.core.Station
import me.viknesh.justaqi.core.StationPicker
import me.viknesh.justaqi.core.distanceKm
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * CPCB stations from data.gov.in, fetched by the phone itself: data.gov.in refuses servers outside
 * India, so no cloud cache can reach it.
 *
 * To keep downloads small the phone only fetches the states that have a station near it. It learns
 * which those are from a station directory (name, state, position) refreshed once a week.
 *
 * The key comes from the DATA_GOV_KEY secret at build time, so it is not in the source. It can
 * still be read out of the APK. ponytail: fine for a free key to public data. Move to a proxy in
 * India if the key gets abused or rate limited.
 */
class StationFeed(context: Context) {
    private val directoryFile = File(context.applicationContext.filesDir, "station_directory.json")

    /** Stations in every state that has one within [StationPicker.MAX_KM]. Empty when no key is built in. */
    suspend fun near(lat: Double, lon: Double): List<Station> = withContext(Dispatchers.IO) {
        if (KEY.isBlank()) return@withContext emptyList()
        val states = directory()
            .filter { distanceKm(lat, lon, it.lat, it.lon) <= StationPicker.MAX_KM }
            .map { it.state }
            .distinct()
        states.flatMap { state ->
            parseRecords(get("filters%5Bstate%5D=${URLEncoder.encode(state, "UTF-8")}&fields=$READING_FIELDS"))
        }
    }

    private data class Entry(val state: String, val lat: Double, val lon: Double)

    /** Cached for a week. A failed refresh keeps the old copy. */
    private fun directory(): List<Entry> {
        val cached = runCatching { JSONObject(directoryFile.readText()) }.getOrNull()
        val fresh = cached != null && System.currentTimeMillis() - cached.getLong("at") < WEEK_MS
        val json = if (fresh) {
            cached!!
        } else {
            runCatching { downloadDirectory() }.getOrNull()?.also { directoryFile.writeText(it.toString()) }
                ?: cached
                ?: return emptyList()
        }
        val arr = json.getJSONArray("stations")
        return List(arr.length()) {
            val s = arr.getJSONObject(it)
            Entry(s.getString("s"), s.getDouble("lat"), s.getDouble("lon"))
        }
    }

    private fun downloadDirectory(): JSONObject {
        val seen = LinkedHashMap<String, JSONObject>()
        var offset = 0
        while (offset < 20_000) {
            val records = JSONObject(get("offset=$offset&fields=state,station,latitude,longitude"))
                .optJSONArray("records") ?: JSONArray()
            for (i in 0 until records.length()) {
                val r = records.getJSONObject(i)
                val lat = r.optString("latitude").toDoubleOrNull() ?: continue
                val lon = r.optString("longitude").toDoubleOrNull() ?: continue
                seen.getOrPut(r.getString("station")) {
                    JSONObject().put("s", r.getString("state")).put("lat", lat).put("lon", lon)
                }
            }
            if (records.length() < PAGE) break
            offset += PAGE
        }
        if (seen.isEmpty()) throw IOException("Station directory came back empty")
        return JSONObject().put("at", System.currentTimeMillis()).put("stations", JSONArray(seen.values))
    }

    private fun get(query: String): String {
        val url = URL("$BASE?api-key=$KEY&format=json&limit=$PAGE&$query")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("data.gov.in returned HTTP ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private val KEY = BuildConfig.DATA_GOV_KEY
        private const val BASE = "https://api.data.gov.in/resource/3b01bcb8-0b14-4abf-b6f2-c1bfd384ba69"
        private const val PAGE = 1000
        private const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L
        private const val READING_FIELDS = "station,latitude,longitude,last_update,pollutant_id,avg_value"
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

        /**
         * One record per station and pollutant, as data.gov.in returns them, grouped into stations.
         * avg_value is the pollutant's CPCB sub-index over the last 24 hours. "NA" values are skipped,
         * as are stations without a usable position.
         */
        internal fun parseRecords(body: String): List<Station> {
            val records = JSONObject(body).optJSONArray("records") ?: return emptyList()
            return (0 until records.length())
                .map { records.getJSONObject(it) }
                .groupBy { it.getString("station") }
                .mapNotNull { (name, rows) ->
                    val first = rows.first()
                    val lat = first.optString("latitude").toDoubleOrNull() ?: return@mapNotNull null
                    val lon = first.optString("longitude").toDoubleOrNull() ?: return@mapNotNull null
                    val subs = LinkedHashMap<Pollutant, Int>()
                    for (row in rows) {
                        val pollutant = IDS[row.optString("pollutant_id")] ?: continue
                        val value = row.optString("avg_value").toDoubleOrNull() ?: continue
                        subs[pollutant] = value.toInt()
                    }
                    val updated = runCatching {
                        LocalDateTime.parse(first.getString("last_update"), STAMP)
                            .atZone(IST).toInstant().toEpochMilli()
                    }.getOrNull()
                    Station(name, lat, lon, updated, subs)
                }
        }
    }
}
