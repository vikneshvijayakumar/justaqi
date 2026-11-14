package me.viknesh.justaqi

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.viknesh.justaqi.core.AqiScale
import me.viknesh.justaqi.data.AirSnapshot
import me.viknesh.justaqi.data.LocationHelper
import me.viknesh.justaqi.data.Prefs
import me.viknesh.justaqi.data.RefreshResult
import me.viknesh.justaqi.data.Repository
import me.viknesh.justaqi.widget.AqiWidget

data class UiState(
    val snapshot: AirSnapshot?,
    val scale: AqiScale,
    /** Chosen city name, or null for "my location". */
    val city: String?,
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
            city = prefs.cityName,
            loading = false,
            error = null,
            hasLocationPermission = LocationHelper.hasPermission(app),
        ),
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun refresh() {
        val app = getApplication<Application>()
        _ui.update {
            it.copy(loading = true, error = null, hasLocationPermission = LocationHelper.hasPermission(app))
        }
        viewModelScope.launch {
            if (prefs.cityName == null) {
                withContext(Dispatchers.IO) { LocationHelper.captureLastKnown(app) }
            }
            when (val result = Repository(app).refresh()) {
                is RefreshResult.Ok -> _ui.update { it.copy(snapshot = result.snapshot, loading = false) }
                is RefreshResult.Failed -> _ui.update { it.copy(loading = false, error = result.message) }
            }
            AqiWidget().updateAll(app)
        }
    }

    fun setScale(scale: AqiScale) {
        prefs.scale = scale
        _ui.update { it.copy(scale = scale) }
        viewModelScope.launch { AqiWidget().updateAll(getApplication<Application>()) }
    }

    /** Pass null to follow the device location. */
    fun setCity(name: String?) {
        prefs.cityName = name
        _ui.update { it.copy(city = name) }
        refresh()
    }

    fun onLocationPermissionResult() {
        refresh()
    }
}
