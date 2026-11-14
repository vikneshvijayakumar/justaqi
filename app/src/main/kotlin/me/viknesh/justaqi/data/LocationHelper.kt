package me.viknesh.justaqi.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {
    private const val FIX_TIMEOUT_MS = 10_000L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Asks the device for a fresh fix, names it, and saves it. Call only while the app is in the
     * foreground. Falls back to the last known fix when a fresh one does not arrive in time.
     * Returns null when permission is missing or the device has no fix at all, in which case the
     * app keeps using the previous location.
     */
    suspend fun captureCurrent(context: Context): Place? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = withTimeoutOrNull(FIX_TIMEOUT_MS) { freshFix(context, manager) }
            ?: lastKnown(manager)
            ?: return null
        val name = withContext(Dispatchers.IO) { placeName(context, location) }
        val place = Place(name, location.latitude, location.longitude)
        Prefs(context).lastLocation = place
        return place
    }

    /** Coarse permission cannot use GPS, so: network provider, else the fused provider (Android 12+). */
    private fun provider(manager: LocationManager): String? = when {
        runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) ->
            LocationManager.NETWORK_PROVIDER
        Build.VERSION.SDK_INT >= 31 &&
            runCatching { manager.isProviderEnabled(LocationManager.FUSED_PROVIDER) }.getOrDefault(false) ->
            LocationManager.FUSED_PROVIDER
        else -> null
    }

    @SuppressLint("MissingPermission")
    private suspend fun freshFix(context: Context, manager: LocationManager): Location? {
        val provider = provider(manager) ?: return null
        return suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(context),
                ) { location: Location? -> if (cont.isActive) cont.resume(location) }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(manager: LocationManager): Location? =
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

    /** Neighborhood or city from the phone's own geocoder. Blank when the phone has none. */
    @Suppress("DEPRECATION")
    private fun placeName(context: Context, location: Location): String = runCatching {
        val address = Geocoder(context, Locale.getDefault())
            .getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()
        listOfNotNull(address?.subLocality, address?.locality ?: address?.subAdminArea)
            .distinct()
            .joinToString(", ")
    }.getOrDefault("")
}
