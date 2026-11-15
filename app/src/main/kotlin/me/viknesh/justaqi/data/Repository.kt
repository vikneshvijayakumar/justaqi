package me.viknesh.justaqi.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import me.viknesh.justaqi.core.CpcbAqi
import me.viknesh.justaqi.core.Pollutant
import me.viknesh.justaqi.core.StationPick
import me.viknesh.justaqi.core.StationPicker
import me.viknesh.justaqi.core.UsAqi
import me.viknesh.justaqi.ui.concText

sealed interface RefreshResult {
    data class Ok(val snapshot: AirSnapshot) : RefreshResult
    data class Failed(val message: String) : RefreshResult
}

class Repository(context: Context) {
    private val prefs = Prefs(context)
    private val stations = StationFeed(context)

    /**
     * Measured station data when a usable station is near (see [StationPicker]), else the
     * modelled estimate. UV always comes from the model. Fails only when both sources fail.
     */
    suspend fun refresh(): RefreshResult {
        val (place, label) = resolvePlace()
        var failure: Exception? = null
        val model = try {
            OpenMeteoClient.fetch(place, label)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure = e
            null
        }
        val pick = try {
            StationPicker.pick(stations.near(place.lat, place.lon), place.lat, place.lon, System.currentTimeMillis())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        val snapshot = when {
            pick != null -> withStation(model ?: emptySnapshot(label), pick)
            model != null -> model
            else -> return RefreshResult.Failed(failure?.message ?: "Couldn't update")
        }
        prefs.snapshot = snapshot
        return RefreshResult.Ok(snapshot)
    }

    private fun emptySnapshot(label: String) = AirSnapshot(
        placeLabel = label, fetchedAtMs = System.currentTimeMillis(), cpcbAqi = null, cpcbDominant = null,
        usAqi = null, pm25 = null, uvNow = null, uvMax = null, uvFromHour = null, uvToHour = null,
    )

    private fun withStation(base: AirSnapshot, pick: StationPick): AirSnapshot {
        val station = pick.station
        // US AQI from the station's PM2.5: turn the CPCB sub-index back into a concentration first.
        val pm25 = station.subIndices[Pollutant.PM25]?.let { CpcbAqi.concentrationFor(Pollutant.PM25, it) }
        return base.copy(
            fetchedAtMs = System.currentTimeMillis(),
            cpcbAqi = station.aqi,
            cpcbDominant = station.dominant,
            usAqi = pm25?.let { UsAqi.pm25(it) },
            pm25 = pm25,
            pollutants = station.subIndices.map { (p, index) -> PollutantReading(p, null, index) },
            source = "${station.name} · ${concText(pick.km)} km",
            measuredAtMs = station.updatedAtMs,
        )
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
