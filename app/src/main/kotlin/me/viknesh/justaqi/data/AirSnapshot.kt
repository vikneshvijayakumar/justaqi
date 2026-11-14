package me.viknesh.justaqi.data

import me.viknesh.justaqi.core.AqiScale
import me.viknesh.justaqi.core.Pollutant
import org.json.JSONObject

/** Everything the app and widget show, saved so the widget can render without a network call. */
data class AirSnapshot(
    val placeLabel: String,
    val fetchedAtMs: Long,
    val cpcbAqi: Int?,
    val cpcbDominant: Pollutant?,
    val usAqi: Int?,
    val pm25: Double?,
    val uvNow: Double?,
    val uvMax: Double?,
    val uvFromHour: Int?,
    val uvToHour: Int?,
) {
    fun aqiFor(scale: AqiScale): Int? = if (scale == AqiScale.CPCB) cpcbAqi else usAqi

    fun toJson(): String {
        val o = JSONObject()
        o.put("placeLabel", placeLabel)
        o.put("fetchedAtMs", fetchedAtMs)
        cpcbAqi?.let { o.put("cpcbAqi", it) }
        cpcbDominant?.let { o.put("cpcbDominant", it.name) }
        usAqi?.let { o.put("usAqi", it) }
        pm25?.let { o.put("pm25", it) }
        uvNow?.let { o.put("uvNow", it) }
        uvMax?.let { o.put("uvMax", it) }
        uvFromHour?.let { o.put("uvFromHour", it) }
        uvToHour?.let { o.put("uvToHour", it) }
        return o.toString()
    }

    companion object {
        fun fromJson(json: String): AirSnapshot? = runCatching {
            val o = JSONObject(json)
            AirSnapshot(
                placeLabel = o.getString("placeLabel"),
                fetchedAtMs = o.getLong("fetchedAtMs"),
                cpcbAqi = o.intOrNull("cpcbAqi"),
                cpcbDominant = if (o.has("cpcbDominant")) Pollutant.valueOf(o.getString("cpcbDominant")) else null,
                usAqi = o.intOrNull("usAqi"),
                pm25 = o.doubleOrNull("pm25"),
                uvNow = o.doubleOrNull("uvNow"),
                uvMax = o.doubleOrNull("uvMax"),
                uvFromHour = o.intOrNull("uvFromHour"),
                uvToHour = o.intOrNull("uvToHour"),
            )
        }.getOrNull()

        private fun JSONObject.intOrNull(key: String): Int? =
            if (has(key) && !isNull(key)) getInt(key) else null

        private fun JSONObject.doubleOrNull(key: String): Double? =
            if (has(key) && !isNull(key)) getDouble(key) else null
    }
}
