package com.malanchuk.svitlo.data

import android.content.Context
import com.malanchuk.svitlo.model.ScheduleSnapshot
import com.malanchuk.svitlo.notifications.NotificationHelper
import com.malanchuk.svitlo.parser.ZoeOutageParser
import com.malanchuk.svitlo.work.ScheduleWorker
import com.malanchuk.svitlo.util.StartupTrace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RefreshResult(
    val snapshot: ScheduleSnapshot,
    val changed: Boolean,
)

class ScheduleRepository(context: Context) {
    private val appContext = context.applicationContext
    private val store = ScheduleStore(appContext)
    private val source = ZoeScheduleSource()
    private val parser = ZoeOutageParser()

    fun load(): ScheduleSnapshot? = store.load()

    fun loadFast(): ScheduleSnapshot? = store.loadFast()

    fun hasFastSnapshot(): Boolean = store.hasFastSnapshot()

    fun selectedQueue(): String = store.selectedQueue()

    fun setSelectedQueue(queue: String) = store.setSelectedQueue(queue)

    fun languageTag(): String = store.languageTag()

    fun setLanguageTag(languageTag: String) = store.setLanguageTag(languageTag)

    fun correction30MinutesEnabled(): Boolean = store.correction30MinutesEnabled()

    fun setCorrection30MinutesEnabled(enabled: Boolean) = store.setCorrection30MinutesEnabled(enabled)

    fun refreshIntervalMinutes(): Int = store.refreshIntervalMinutes()

    fun setRefreshIntervalMinutes(minutes: Int): Boolean {
        if (minutes !in MIN_REFRESH_INTERVAL..MAX_REFRESH_INTERVAL) return false
        store.setRefreshIntervalMinutes(minutes)
        ScheduleWorker.schedule(appContext, minutes, replaceExisting = true)
        return true
    }

    suspend fun refresh(): RefreshResult = withContext(Dispatchers.IO) {
        StartupTrace.mark("repository refresh entered IO")
        val previous = store.load()
        StartupTrace.mark("local snapshot loaded")
        val html = source.fetchHtml()
        StartupTrace.mark("source HTML received (${html.length} chars)")
        val snapshot = parser.parse(html)
        StartupTrace.mark("HTML parsed")
        if (snapshot.days.isEmpty() || snapshot.days.none { it.queues.isNotEmpty() }) {
            error("ZOE response did not contain outage schedules")
        }
        store.save(snapshot)
        if (previous != null && previous.digestFor(store.selectedQueue()) != snapshot.digestFor(store.selectedQueue())) {
            NotificationHelper.showScheduleChanged(appContext, store.selectedQueue())
        }
        RefreshResult(
            snapshot = snapshot,
            changed = previous?.digestFor(store.selectedQueue()) != snapshot.digestFor(store.selectedQueue()),
        )
    }

    private companion object {
        const val MIN_REFRESH_INTERVAL = 15
        const val MAX_REFRESH_INTERVAL = 1440
    }
}
