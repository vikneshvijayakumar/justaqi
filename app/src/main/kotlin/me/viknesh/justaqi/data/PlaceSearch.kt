package me.viknesh.justaqi.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Place search by name, using Photon (photon.komoot.io): OpenStreetMap data, no key, built for
 * search-as-you-type, and it knows neighborhoods and villages, not only cities.
 * ponytail: the public Photon server is fair-use. Self-host or switch provider if it throttles.
 * India only (bounding box plus country check) because the app is built around the CPCB scale.
 */
object PlaceSearch {
    private const val BASE = "https://photon.komoot.io/api/"

    /** Returns an empty list for short queries, no matches, or a failed request. */
    suspend fun search(query: String): List<Place> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        runCatching {
            val url = URL("$BASE?q=${URLEncoder.encode(q, "UTF-8")}&limit=15&lang=en&bbox=68,6,98,37")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "justaqi-android/0.1 (open source; github.com/vikneshvijayakumar/justaqi)")
            }
            try {
                parse(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(emptyList())
    }

    internal fun parse(body: String): List<Place> {
        val features = JSONObject(body).optJSONArray("features") ?: return emptyList()
        return (0 until features.length()).mapNotNull { i ->
            val f = features.getJSONObject(i)
            val props = f.optJSONObject("properties") ?: return@mapNotNull null
            val xy = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return@mapNotNull null
            if (props.optString("countrycode") != "IN") return@mapNotNull null
            val name = listOf("name", "city", "county", "state")
                .map { props.optString(it) }
                .filter { it.isNotBlank() }
                .distinct()
                .take(3)
                .joinToString(", ")
            if (name.isBlank()) null else Place(name, xy.getDouble(1), xy.getDouble(0))
        }.distinctBy { it.name }
    }
}
