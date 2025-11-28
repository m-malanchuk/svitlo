package com.malanchuk.svitlo.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.malanchuk.svitlo.R
import com.malanchuk.svitlo.data.ScheduleStore
import com.malanchuk.svitlo.util.withLanguage

object NotificationHelper {
    private const val CHANNEL_ID = "schedule_updates"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val localizedContext = context.localizedAppContext()
        val manager = localizedContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                localizedContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = localizedContext.getString(R.string.notification_channel_description)
            },
        )
    }

    fun showScheduleChanged(context: Context, queue: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val localizedContext = context.localizedAppContext()
        ensureChannel(localizedContext)
        val notification = NotificationCompat.Builder(localizedContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(localizedContext.getString(R.string.notification_title))
            .setContentText(localizedContext.getString(R.string.notification_text, queue))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(localizedContext).notify(NOTIFICATION_ID, notification)
    }

    private fun Context.localizedAppContext(): Context =
        withLanguage(ScheduleStore(this).languageTag())
}
