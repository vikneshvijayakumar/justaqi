package me.viknesh.justaqi.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import me.viknesh.justaqi.work.RefreshWorker

class AqiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AqiWidget()

    /** First widget placed on the home screen: start background refreshes and fetch right away. */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RefreshWorker.schedule(context)
        RefreshWorker.refreshNow(context)
    }
}
