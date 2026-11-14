package me.viknesh.justaqi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.viknesh.justaqi.UiState
import me.viknesh.justaqi.core.AqiScale
import me.viknesh.justaqi.core.Sunscreen
import me.viknesh.justaqi.data.Places
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onScale: (AqiScale) -> Unit,
    onCity: (String?) -> Unit,
    onRequestLocation: () -> Unit,
) {
    MaterialTheme(colorScheme = lightColorScheme()) {
        val snapshot = state.snapshot
        val aqi = snapshot?.aqiFor(state.scale)
        val band = aqi?.let { Palette.band(state.scale, it) }
        val bg = band?.bg ?: Palette.NEUTRAL_BG
        val fg = band?.fg ?: Palette.INK
        val muted = fg.copy(alpha = 0.8f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top: where, and which scale.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PlacePicker(
                    label = snapshot?.placeLabel ?: "Choose place",
                    color = fg,
                    onCity = onCity,
                )
                ScaleToggle(scale = state.scale, color = fg, onScale = onScale)
            }

            // Middle: the number.
            Column {
                val label = aqi?.let { Palette.label(state.scale, it) }
                Column(
                    modifier = Modifier.semantics {
                        contentDescription = if (aqi != null) {
                            "Air quality index $aqi, $label"
                        } else {
                            "Air quality not available yet"
                        }
                    },
                ) {
                    Text(
                        text = aqi?.toString() ?: "–",
                        color = fg,
                        fontSize = 128.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = label ?: "",
                        color = fg,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                val dominant = snapshot?.cpcbDominant
                if (state.scale == AqiScale.CPCB && dominant != null) {
                    Text(
                        text = "Main pollutant: ${dominant.label}",
                        color = muted,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Text(
                    text = statusLine(state),
                    color = muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (state.city == null && !state.hasLocationPermission) {
                    Text(
                        text = "Allow location to see air quality where you are",
                        color = fg,
                        fontSize = 14.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clickable(onClick = onRequestLocation),
                    )
                }
            }

            // Bottom: sun, then the honest footnote.
            Column {
                SunBlock(state = state, fg = fg, muted = muted)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Modelled estimate · Open-Meteo",
                        color = muted,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = if (state.loading) "Updating…" else "Refresh",
                        color = fg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(enabled = !state.loading, onClick = onRefresh),
                    )
                }
            }
        }
    }
}

private fun statusLine(state: UiState): String {
    val snapshot = state.snapshot
    return when {
        state.loading && snapshot == null -> "Updating…"
        state.error != null && snapshot != null -> "Couldn't update. Showing data from ${agoText(snapshot.fetchedAtMs)}"
        state.error != null -> "Couldn't update. Check your connection."
        snapshot != null -> "Updated ${agoText(snapshot.fetchedAtMs)}"
        else -> ""
    }
}

@Composable
private fun PlacePicker(label: String, color: Color, onCity: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Text(
            text = "$label ▾",
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable { open = true }
                .padding(vertical = 8.dp),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("My location") },
                onClick = {
                    open = false
                    onCity(null)
                },
            )
            Places.cities.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city.name) },
                    onClick = {
                        open = false
                        onCity(city.name)
                    },
                )
            }
        }
    }
}

@Composable
private fun ScaleToggle(scale: AqiScale, color: Color, onScale: (AqiScale) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(AqiScale.CPCB to "CPCB", AqiScale.US to "US").forEach { (option, name) ->
            val selected = option == scale
            Text(
                text = name,
                color = color,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textDecoration = if (selected) TextDecoration.Underline else TextDecoration.None,
                modifier = Modifier
                    .clickable { onScale(option) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun SunBlock(state: UiState, fg: Color, muted: Color) {
    val snapshot = state.snapshot ?: return
    val peak = snapshot.uvMax ?: return
    val advice = Sunscreen.advise(peak)
    val window = if (snapshot.uvFromHour != null && snapshot.uvToHour != null) {
        "UV 3+ from ${hourLabel(snapshot.uvFromHour)} to ${hourLabel(snapshot.uvToHour + 1)}"
    } else {
        "UV stays low today"
    }

    Column {
        Text(
            text = "Sun · peak UV ${peak.roundToInt()} · ${advice.level.label}",
            color = muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = advice.headline,
            color = fg,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = advice.detail,
            color = muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = window,
            color = muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
