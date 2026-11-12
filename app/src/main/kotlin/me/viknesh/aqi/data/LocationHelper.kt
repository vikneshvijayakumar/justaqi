package me.viknesh.aqi.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

object LocationHelper {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Saves the most recent known location. Call only while the app is in the foreground.
     * Returns null when permission is missing or the device has no recent fix, in which case
     * the app keeps using the previous location or the Delhi default.
     */
    @SuppressLint("MissingPermission")
    fun captureLastKnown(context: Context): Place? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?: return null
        val place = Place("", location.latitude, location.longitude)
        Prefs(context).lastLocation = place
        return place
    }
}
