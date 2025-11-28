package com.malanchuk.svitlo.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.malanchuk.svitlo.data.ScheduleRepository
import com.malanchuk.svitlo.data.ScheduleUpdateBus
import java.util.concurrent.TimeUnit

class ScheduleWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = runCatching {
        ScheduleRepository(applicationContext).refresh()
    }.fold(
        onSuccess = { result ->
            ScheduleUpdateBus.publish(result.snapshot)
            Result.success()
        },
        onFailure = { Result.retry() },
    )

    companion object {
        const val WORK_NAME = "svitlo-schedule-refresh"
        const val DEFAULT_INTERVAL_MINUTES = 15

        fun schedule(context: Context, intervalMinutes: Int, replaceExisting: Boolean) {
            val safeInterval = intervalMinutes.coerceIn(15, 1440)
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                if (replaceExisting) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                else ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ScheduleWorker>(safeInterval.toLong(), TimeUnit.MINUTES)
                    .setInitialDelay(safeInterval.toLong(), TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    )
                    .build(),
            )
        }
    }
}
