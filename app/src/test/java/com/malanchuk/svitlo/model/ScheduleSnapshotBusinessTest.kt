package com.malanchuk.svitlo.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleSnapshotBusinessTest {
    @Test
    fun digestIsStableWhenQueueMapInsertionOrderChanges() {
        val first = snapshot(
            queues = linkedMapOf(
                "2.1" to listOf(OutageInterval(120, 240)),
                "1.1" to listOf(OutageInterval(0, 60)),
            ),
        )
        val second = snapshot(
            queues = linkedMapOf(
                "1.1" to listOf(OutageInterval(0, 60)),
                "2.1" to listOf(OutageInterval(120, 240)),
            ),
        )

        assertEquals(first.digestFor("1.1"), second.digestFor("1.1"))
    }

    @Test
    fun digestChangesWhenSelectedQueueScheduleChanges() {
        val first = snapshot(queues = mapOf("1.1" to listOf(OutageInterval(0, 60))))
        val second = snapshot(queues = mapOf("1.1" to listOf(OutageInterval(30, 90))))

        assertNotEquals(first.digestFor("1.1"), second.digestFor("1.1"))
    }

    @Test
    fun queueNamesContainDefaultsAndDiscoveredQueuesInNaturalOrder() {
        val snapshot = snapshot(
            queues = mapOf(
                "10.1" to emptyList(),
                "2.2" to emptyList(),
            ),
        )

        val names = snapshot.queueNames()

        assertTrue(names.containsAll(listOf("1.1", "1.2", "6.2", "10.1")))
        assertEquals(listOf("1.1", "1.2", "2.1", "2.2"), names.take(4))
    }

    private fun snapshot(queues: Map<String, List<OutageInterval>>) = ScheduleSnapshot(
        days = listOf(
            DailySchedule(
                date = LocalDate.of(2026, 11, 8),
                queues = queues,
                articleId = "post-test",
                sourceUpdatedAt = null,
            ),
        ),
        fetchedAt = Instant.parse("2026-11-08T07:00:00Z"),
        sourceUrl = "https://www.zoe.com.ua/outage/",
    )
}
