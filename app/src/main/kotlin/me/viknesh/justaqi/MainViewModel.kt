package me.viknesh.justaqi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.viknesh.justaqi.core.AqiScale
import me.viknesh.justaqi.data.AirSnapshot
import me.viknesh.justaqi.data.LocationHelper
import me.viknesh.justaqi.data.Place
import me.viknesh.justaqi.data.Prefs
import me.viknesh.justaqi.data.RefreshResult
import me.viknesh.justaqi.data.Repository
import me.viknesh.justaqi.widget.updateAllWidgets

data class UiState(
    val snapshot: AirSnapshot?,
    val scale: AqiScale,
    /** True when the app follows the device location, false when a place was picked by search. */
    val followsLocation: Boolean,
    val loading: Boolean,
    val error: String?,
    val hasLocationPermission: Boolean,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    private val _ui = MutableStateFlow(
        UiState(
            snapshot = prefs.snapshot,
            scale = prefs.scale,
            followsLocation = prefs.chosenPlace == null,
            loading = false,
            error = null,
            hasLocationPermission = LocationHelper.hasPermission(app),
        ),
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /** True once, on the first launch, when the app should ask for location. */
    fun shouldAskLocation(): Boolean {
        val app = getApplication<Application>()
        if (prefs.askedLocation || prefs.chosenPlace != null || LocationHelper.hasPermission(app)) return false
        prefs.askedLocation = true
        return true
    }

    fun refresh() {
        val app = getApplication<Application>()
        _ui.update {
            it.copy(loading = true, error = null, hasLocationPermission = LocationHelper.hasPermission(app))
        }
        viewModelScope.launch {
            if (prefs.chosenPlace == null) LocationHelper.captureCurrent(app)
            when (val result = Repository(app).refresh()) {
                is RefreshResult.Ok -> _ui.update { it.copy(snapshot = result.snapshot, loading = false) }
                is RefreshResult.Failed -> _ui.update { it.copy(loading = false, error = result.message) }
            }
            updateAllWidgets(app)
        }
    }

    fun setScale(scale: AqiScale) {
        prefs.scale = scale
        _ui.update { it.copy(scale = scale) }
        viewModelScope.launch { updateAllWidgets(getApplication<Application>()) }
    }

    /** Pass null to follow the device location. */
    fun setPlace(place: Place?) {
        prefs.chosenPlace = place
        _ui.update { it.copy(followsLocation = place == null) }
        refresh()
    }
}
