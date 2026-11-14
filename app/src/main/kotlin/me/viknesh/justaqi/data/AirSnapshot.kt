package me.viknesh.justaqi.data

import me.viknesh.justaqi.core.AqiScale
import me.viknesh.justaqi.core.Pollutant
import org.json.JSONArray
import org.json.JSONObject

/**
 * One row of the breakdown. [concentration] is the CPCB averaging-window mean (µg/m³, CO in mg/m³).
 * It is null for station data, which arrives as sub-indices only.
 */
data class PollutantReading(val pollutant: Pollutant, val concentration: Double?, val subIndex: Int) {
    val unit: String get() = if (pollutant == Pollutant.CO) "mg/m³" else "µg/m³"
}

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
    val pollutants: List<PollutantReading> = emptyList(),
    /** Station name and distance when the numbers are measured. Null means modelled estimate. */
    val source: String? = null,
    /** When the station last reported. Null for modelled data. */
    val measuredAtMs: Long? = null,
) {
    /** The time the numbers describe: the station's report time, else when we fetched. */
    val asOfMs: Long get() = measuredAtMs ?: fetchedAtMs

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
        val arr = JSONArray()
        pollutants.forEach {
            arr.put(JSONObject().put("p", it.pollutant.name).putOpt("c", it.concentration).put("i", it.subIndex))
        }
        o.put("pollutants", arr)
        source?.let { o.put("source", it) }
        measuredAtMs?.let { o.put("measuredAtMs", it) }
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
                pollutants = o.optJSONArray("pollutants")?.let { arr ->
                    List(arr.length()) {
                        val r = arr.getJSONObject(it)
                        PollutantReading(Pollutant.valueOf(r.getString("p")), r.doubleOrNull("c"), r.getInt("i"))
                    }
                } ?: emptyList(),
                source = if (o.has("source")) o.getString("source") else null,
                measuredAtMs = if (o.has("measuredAtMs")) o.getLong("measuredAtMs") else null,
            )
        }.getOrNull()

        private fun JSONObject.intOrNull(key: String): Int? =
            if (has(key) && !isNull(key)) getInt(key) else null

        private fun JSONObject.doubleOrNull(key: String): Double? =
            if (has(key) && !isNull(key)) getDouble(key) else null
    }
}
