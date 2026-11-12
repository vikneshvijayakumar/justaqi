package me.viknesh.aqi.data

import android.content.Context
import androidx.core.content.edit
import me.viknesh.aqi.core.AqiScale

/** Small SharedPreferences wrapper. The widget reads the cached snapshot from here. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("aqi_prefs", Context.MODE_PRIVATE)

    var scale: AqiScale
        get() = runCatching { AqiScale.valueOf(sp.getString(KEY_SCALE, null) ?: "") }
            .getOrDefault(AqiScale.CPCB)
        set(value) = sp.edit { putString(KEY_SCALE, value.name) }

    /** Name of a chosen city from [Places.cities], or null to use the device location. */
    var cityName: String?
        get() = sp.getString(KEY_CITY, null)
        set(value) = sp.edit {
            if (value == null) remove(KEY_CITY) else putString(KEY_CITY, value)
        }

    /**
     * Last location the app saw while it was open. The widget refreshes in the background and
     * uses this, so the app never needs the background-location permission.
     */
    var lastLocation: Place?
        get() = sp.getString(KEY_LOCATION, null)?.split(",")?.let { parts ->
            val lat = parts.getOrNull(0)?.toDoubleOrNull()
            val lon = parts.getOrNull(1)?.toDoubleOrNull()
            if (lat != null && lon != null) Place("", lat, lon) else null
        }
        set(value) = sp.edit {
            if (value == null) remove(KEY_LOCATION) else putString(KEY_LOCATION, "${value.lat},${value.lon}")
        }

    var snapshot: AirSnapshot?
        get() = sp.getString(KEY_SNAPSHOT, null)?.let { AirSnapshot.fromJson(it) }
        set(value) = sp.edit {
            if (value == null) remove(KEY_SNAPSHOT) else putString(KEY_SNAPSHOT, value.toJson())
        }

    private companion object {
        const val KEY_SCALE = "scale"
        const val KEY_CITY = "city"
        const val KEY_LOCATION = "last_location"
        const val KEY_SNAPSHOT = "snapshot"
    }
}
