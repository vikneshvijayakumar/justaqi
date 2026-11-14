package me.viknesh.justaqi.data

import android.content.Context
import androidx.core.content.edit
import me.viknesh.justaqi.core.AqiScale

/** Small SharedPreferences wrapper. The widget reads the cached snapshot from here. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("aqi_prefs", Context.MODE_PRIVATE)

    var scale: AqiScale
        get() = runCatching { AqiScale.valueOf(sp.getString(KEY_SCALE, null) ?: "") }
            .getOrDefault(AqiScale.CPCB)
        set(value) = sp.edit { putString(KEY_SCALE, value.name) }

    /** A place picked by search, or null to follow the device location. */
    var chosenPlace: Place?
        get() = readPlace(KEY_CHOSEN)
        set(value) = writePlace(KEY_CHOSEN, value)

    /**
     * Last location the app saw while it was open. The widget refreshes in the background and
     * uses this, so the app never needs the background-location permission.
     */
    var lastLocation: Place?
        get() = readPlace(KEY_LOCATION)
        set(value) = writePlace(KEY_LOCATION, value)

    /** True once the first-launch location prompt has been shown, so it is never shown twice. */
    var askedLocation: Boolean
        get() = sp.getBoolean(KEY_ASKED, false)
        set(value) = sp.edit { putBoolean(KEY_ASKED, value) }

    // Stored as "lat,lon,name". The name comes last because it may contain commas.
    private fun readPlace(key: String): Place? {
        val parts = sp.getString(key, null)?.split(",", limit = 3) ?: return null
        val lat = parts.getOrNull(0)?.toDoubleOrNull() ?: return null
        val lon = parts.getOrNull(1)?.toDoubleOrNull() ?: return null
        return Place(parts.getOrNull(2).orEmpty(), lat, lon)
    }

    private fun writePlace(key: String, value: Place?) = sp.edit {
        if (value == null) remove(key) else putString(key, "${value.lat},${value.lon},${value.name}")
    }

    var snapshot: AirSnapshot?
        get() = sp.getString(KEY_SNAPSHOT, null)?.let { AirSnapshot.fromJson(it) }
        set(value) = sp.edit {
            if (value == null) remove(KEY_SNAPSHOT) else putString(KEY_SNAPSHOT, value.toJson())
        }

    private companion object {
        const val KEY_SCALE = "scale"
        const val KEY_CHOSEN = "chosen_place"
        const val KEY_ASKED = "asked_location"
        const val KEY_LOCATION = "last_location"
        const val KEY_SNAPSHOT = "snapshot"
    }
}
