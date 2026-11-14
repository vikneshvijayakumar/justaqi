package me.viknesh.justaqi.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Place search by name, using Open-Meteo's geocoding API (GeoNames data, no key).
 * ponytail: India only (countryCode=IN) because the app is built around the CPCB scale.
 * Drop that parameter to search worldwide.
 */
object PlaceSearch {
    private const val BASE = "https://geocoding-api.open-meteo.com/v1/search"

    /** Returns an empty list for short queries, no matches, or a failed request. */
    suspend fun search(query: String): List<Place> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        runCatching {
            val url = URL("$BASE?name=${URLEncoder.encode(q, "UTF-8")}&count=10&language=en&format=json&countryCode=IN")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "justaqi-android/0.1 (open source)")
            }
            try {
                parse(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(emptyList())
    }

    internal fun parse(body: String): List<Place> {
        val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
        return List(results.length()) {
            val r = results.getJSONObject(it)
            val name = listOf(r.getString("name"), r.optString("admin1"))
                .filter { part -> part.isNotBlank() }
                .distinct()
                .joinToString(", ")
            Place(name, r.getDouble("latitude"), r.getDouble("longitude"))
        }
    }
}
