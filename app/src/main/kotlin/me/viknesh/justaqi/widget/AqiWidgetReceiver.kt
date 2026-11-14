package me.viknesh.justaqi.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import me.viknesh.justaqi.work.RefreshWorker

/** First widget placed on the home screen: start background refreshes and fetch right away. */
private fun startRefreshing(context: Context) {
    RefreshWorker.schedule(context)
    RefreshWorker.refreshNow(context)
}

class AqiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AqiWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        startRefreshing(context)
    }
}

class AqiDetailWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AqiDetailWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        startRefreshing(context)
    }
}
