package me.viknesh.aqi

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import me.viknesh.aqi.ui.HomeScreen
import me.viknesh.aqi.work.RefreshWorker

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
            ) { viewModel.onLocationPermissionResult() }

            HomeScreen(
                state = state,
                onRefresh = viewModel::refresh,
                onScale = viewModel::setScale,
                onCity = viewModel::setCity,
                onRequestLocation = { locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            )
        }

        viewModel.refresh()
    }
}
