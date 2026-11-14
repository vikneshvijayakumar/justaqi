package me.viknesh.justaqi.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import me.viknesh.justaqi.data.RefreshResult
import me.viknesh.justaqi.data.Repository
import me.viknesh.justaqi.widget.updateAllWidgets
import java.util.concurrent.TimeUnit

/** Fetches fresh data and redraws the widget. Runs about every 30 minutes while the device has network. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val outcome = Repository(applicationContext).refresh()
        updateAllWidgets(applicationContext)
        return if (outcome is RefreshResult.Ok) Result.success() else Result.retry()
    }

    companion object {
        private const val PERIODIC_NAME = "aqi-periodic-refresh"

        private val needsNetwork: Constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Safe to call repeatedly: an existing schedule is kept. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(needsNetwork)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun refreshNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>()
                .setConstraints(needsNetwork)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
