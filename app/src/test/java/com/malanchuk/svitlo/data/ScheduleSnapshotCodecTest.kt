package com.malanchuk.svitlo.data

import com.malanchuk.svitlo.model.DailySchedule
import com.malanchuk.svitlo.model.OutageInterval
import com.malanchuk.svitlo.model.ScheduleSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleSnapshotCodecTest {
    @Test
    fun roundTripsSnapshotWithoutTextParsing() {
        val snapshot = ScheduleSnapshot(
            days = listOf(
                DailySchedule(
                    date = LocalDate.of(2026, 11, 7),
                    queues = linkedMapOf(
                        "1.1" to listOf(OutageInterval(0, 120), OutageInterval(1380, 1440)),
                        "2.2" to emptyList(),
                    ),
                    articleId = "post-123",
                    sourceUpdatedAt = LocalTime.of(21, 25),
                ),
            ),
            fetchedAt = Instant.parse("2026-11-07T07:00:00Z"),
            sourceUrl = "https://www.zoe.com.ua/outage/",
        )

        assertEquals(snapshot, ScheduleSnapshotCodec.decode(ScheduleSnapshotCodec.encode(snapshot)))
    }
}
