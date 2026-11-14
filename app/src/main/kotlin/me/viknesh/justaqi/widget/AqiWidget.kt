package me.viknesh.justaqi.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import me.viknesh.justaqi.MainActivity
import me.viknesh.justaqi.core.AqiScale
import me.viknesh.justaqi.data.AirSnapshot
import me.viknesh.justaqi.data.Prefs
import me.viknesh.justaqi.ui.Band
import me.viknesh.justaqi.ui.Palette
import me.viknesh.justaqi.ui.agoText
import me.viknesh.justaqi.ui.concText

/** Redraws every placed widget of both kinds from the saved snapshot. */
suspend fun updateAllWidgets(context: Context) {
    AqiWidget().updateAll(context)
    AqiDetailWidget().updateAll(context)
}

/**
 * Small widget: the number, its category, the place, the scale and how fresh it is.
 * States: no data yet, fresh, and old (data more than 2 hours old, say so plainly).
 */
class AqiWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = Prefs(context)
        val snapshot = prefs.snapshot
        val scale = prefs.scale
        provideContent { WidgetFrame(snapshot, scale, detailed = false) }
    }
}

/** Wide widget: the same summary on the left, every pollutant on the right. */
class AqiDetailWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = Prefs(context)
        val snapshot = prefs.snapshot
        val scale = prefs.scale
        provideContent { WidgetFrame(snapshot, scale, detailed = true) }
    }
}

private const val STALE_AFTER_MS = 2 * 60 * 60 * 1000L

@Composable
private fun WidgetFrame(snapshot: AirSnapshot?, scale: AqiScale, detailed: Boolean) {
    val aqi = snapshot?.aqiFor(scale)
    val band = aqi?.let { Palette.band(scale, it) }
    val frame = GlanceModifier
        .fillMaxSize()
        .background(ColorProvider(band?.bg ?: Palette.NEUTRAL_BG))
        .cornerRadius(24.dp)
        .padding(12.dp)
        .clickable(actionStartActivity<MainActivity>())

    if (snapshot == null || aqi == null || band == null) {
        Column(
            modifier = frame,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Tap to set up",
                style = TextStyle(
                    color = ColorProvider(Palette.INK),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        return
    }

    if (!detailed) {
        Column(
            modifier = frame,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Summary(snapshot, scale, aqi, band)
        }
        return
    }

    Row(modifier = frame, verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Summary(snapshot, scale, aqi, band)
        }
        Spacer(modifier = GlanceModifier.width(12.dp))
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (snapshot.pollutants.isEmpty()) {
                Text(
                    text = "Open the app to load pollutants",
                    style = TextStyle(color = ColorProvider(band.fg), fontSize = 11.sp),
                )
            }
            snapshot.pollutants.forEach { reading ->
                val weight =
                    if (reading.pollutant == snapshot.cpcbDominant) FontWeight.Bold else FontWeight.Normal
                val style = TextStyle(color = ColorProvider(band.fg), fontSize = 11.sp, fontWeight = weight)
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = reading.pollutant.label,
                        style = style,
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        text = "${concText(reading.concentration)} ${reading.unit}",
                        style = style,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun Summary(snapshot: AirSnapshot, scale: AqiScale, aqi: Int, band: Band) {
    val old = System.currentTimeMillis() - snapshot.fetchedAtMs > STALE_AFTER_MS
    val scaleName = if (scale == AqiScale.CPCB) "CPCB" else "US AQI"
    val freshness = (if (old) "Old · " else "") + agoText(snapshot.fetchedAtMs)
    val small = TextStyle(color = ColorProvider(band.fg), fontSize = 11.sp)

    Text(
        text = aqi.toString(),
        style = TextStyle(color = ColorProvider(band.fg), fontSize = 48.sp, fontWeight = FontWeight.Bold),
    )
    Text(
        text = Palette.shortLabel(scale, aqi),
        style = TextStyle(color = ColorProvider(band.fg), fontSize = 14.sp, fontWeight = FontWeight.Medium),
        maxLines = 1,
    )
    Spacer(modifier = GlanceModifier.height(4.dp))
    Text(text = snapshot.placeLabel, style = small, maxLines = 1)
    Text(text = "$scaleName · $freshness", style = small, maxLines = 1)
}
