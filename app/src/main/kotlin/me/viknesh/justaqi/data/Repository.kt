package me.viknesh.justaqi.data

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

    /** Chosen place, else the last location the app saw, else Delhi. */
    fun resolvePlace(): Pair<Place, String> {
        prefs.chosenPlace?.let { return it to it.name }

        val last = prefs.lastLocation
        if (last != null) {
            val label = last.name.ifBlank {
                Places.nearest(last.lat, last.lon)?.let { "Near ${it.name}" } ?: "Your location"
            }
            return last to label
        }
        return Places.DELHI to Places.DELHI.name
    }
}
