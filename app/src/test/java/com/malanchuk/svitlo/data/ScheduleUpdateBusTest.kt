package com.malanchuk.svitlo.data

import com.malanchuk.svitlo.model.DailySchedule
import com.malanchuk.svitlo.model.ScheduleSnapshot
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleUpdateBusTest {
    @Test
    fun publishesPersistedSnapshotToVisibleObservers() = runBlocking {
        val snapshot = ScheduleSnapshot(
            days = listOf(
                DailySchedule(
                    date = LocalDate.of(2026, 11, 9),
                    queues = emptyMap(),
                    articleId = "post-test",
                    sourceUpdatedAt = null,
                ),
            ),
            fetchedAt = Instant.parse("2026-11-09T16:00:00Z"),
            sourceUrl = "https://www.zoe.com.ua/outage/",
        )
        val observed = async(start = CoroutineStart.UNDISPATCHED) {
            ScheduleUpdateBus.updates.first()
        }

        ScheduleUpdateBus.publish(snapshot)

        assertEquals(snapshot, observed.await())
    }
}
