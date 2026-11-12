package me.viknesh.aqi.data

import android.content.Context
import kotlinx.coroutines.CancellationException

sealed interface RefreshResult {
    data class Ok(val snapshot: AirSnapshot) : RefreshResult
    data class Failed(val message: String) : RefreshResult
}

class Repository(context: Context) {
    private val prefs = Prefs(context)

    suspend fun refresh(): RefreshResult {
        return try {
            val (place, label) = resolvePlace()
            val snapshot = OpenMeteoClient.fetch(place, label)
            prefs.snapshot = snapshot
            RefreshResult.Ok(snapshot)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RefreshResult.Failed(e.message ?: "Couldn't update")
        }
    }

    /** Chosen city, else the last location the app saw, else Delhi. */
    fun resolvePlace(): Pair<Place, String> {
        val chosen = prefs.cityName?.let { name -> Places.cities.firstOrNull { it.name == name } }
        if (chosen != null) return chosen to chosen.name

        val last = prefs.lastLocation
        if (last != null) {
            val near = Places.nearest(last.lat, last.lon)
            return last to (near?.let { "Near ${it.name}" } ?: "Your location")
        }
        return Places.DELHI to Places.DELHI.name
    }
}
