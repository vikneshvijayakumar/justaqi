package me.viknesh.aqi.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import me.viknesh.aqi.MainActivity
import me.viknesh.aqi.core.AqiScale
import me.viknesh.aqi.data.AirSnapshot
import me.viknesh.aqi.data.Prefs
import me.viknesh.aqi.ui.Palette
import me.viknesh.aqi.ui.agoText

/**
 * Home-screen widget: the number, its category, and where and how fresh it is. Nothing else.
 * States: no data yet, fresh, and old (data more than 2 hours old, say so plainly).
 */
class AqiWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = Prefs(context)
        val snapshot = prefs.snapshot
        val scale = prefs.scale
        provideContent { WidgetContent(snapshot, scale) }
    }
}

private const val STALE_AFTER_MS = 2 * 60 * 60 * 1000L

@Composable
private fun WidgetContent(snapshot: AirSnapshot?, scale: AqiScale) {
    val open = actionStartActivity<MainActivity>()
    val aqi = snapshot?.aqiFor(scale)

    if (snapshot == null || aqi == null) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Palette.NEUTRAL_BG))
                .cornerRadius(24.dp)
                .padding(12.dp)
                .clickable(open),
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

    val band = Palette.band(scale, aqi)
    val age = System.currentTimeMillis() - snapshot.fetchedAtMs
    val footer = if (age > STALE_AFTER_MS) {
        "Old data · ${agoText(snapshot.fetchedAtMs)}"
    } else {
        "${snapshot.placeLabel} · ${agoText(snapshot.fetchedAtMs)}"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(band.bg))
            .cornerRadius(24.dp)
            .padding(12.dp)
            .clickable(open),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = aqi.toString(),
            style = TextStyle(
                color = ColorProvider(band.fg),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = Palette.shortLabel(scale, aqi),
            style = TextStyle(
                color = ColorProvider(band.fg),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = footer,
            style = TextStyle(
                color = ColorProvider(band.fg),
                fontSize = 11.sp,
            ),
            maxLines = 1,
        )
    }
}
