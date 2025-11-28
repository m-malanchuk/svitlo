package com.malanchuk.svitlo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.malanchuk.svitlo.data.ScheduleStore
import com.malanchuk.svitlo.notifications.NotificationHelper
import com.malanchuk.svitlo.util.StartupTrace
import com.malanchuk.svitlo.work.ScheduleWorker

class SvitloApplication : Application() {
    override fun onCreate() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate()
        StartupTrace.reset()
        StartupTrace.mark("Application.onCreate start")
        NotificationHelper.ensureChannel(this)
        StartupTrace.mark("notification channel ready")
        ScheduleWorker.schedule(this, ScheduleStore(this).refreshIntervalMinutes(), replaceExisting = false)
        StartupTrace.mark("WorkManager schedule requested")
    }
}
