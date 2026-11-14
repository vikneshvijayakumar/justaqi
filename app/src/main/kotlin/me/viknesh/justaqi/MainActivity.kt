package me.viknesh.justaqi

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import me.viknesh.justaqi.data.LocationHelper
import me.viknesh.justaqi.data.PlaceSearch
import me.viknesh.justaqi.ui.HomeScreen
import me.viknesh.justaqi.work.RefreshWorker

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RefreshWorker.schedule(this)

        setContent {
            val state by viewModel.ui.collectAsState()
            val locationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { viewModel.refresh() }
            val askLocation = { locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }

            // First launch: ask for location once instead of silently showing Delhi.
            LaunchedEffect(Unit) { if (viewModel.shouldAskLocation()) askLocation() }

            HomeScreen(
                state = state,
                onRefresh = viewModel::refresh,
                onScale = viewModel::setScale,
                onPlace = { place ->
                    viewModel.setPlace(place)
                    if (place == null && !LocationHelper.hasPermission(this)) askLocation()
                },
                onSearch = PlaceSearch::search,
                onRequestLocation = askLocation,
            )
        }
    }

    /** Every time the app comes to the front: fresh location, fresh data. */
    override fun onStart() {
        super.onStart()
        viewModel.refresh()
    }
}
