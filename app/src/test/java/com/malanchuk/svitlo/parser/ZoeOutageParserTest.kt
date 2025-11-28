package com.malanchuk.svitlo.parser

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoeOutageParserTest {
    private val clock = Clock.fixed(
        Instant.parse("2026-11-07T07:00:00Z"),
        ZoneId.of("Europe/Kyiv"),
    )

    @Test
    fun parsesCurrentDayQueuesAnd24HourEnd() {
        val snapshot = parser().parse(fixture())
        val day = snapshot.day(LocalDate.of(2026, 11, 7))

        assertNotNull(day)
        val queue = day!!.queues["1.1"]!!
        assertEquals(listOf(0 to 120, 360 to 660, 900 to 1200), queue.map { it.startMinute to it.endMinute })
        assertEquals(12, day.queues.size)
        assertEquals(1440, day.queues["6.2"]!!.last().endMinute)
    }

    @Test
    fun selectsLatestRevisionForTheSameDate() {
        val snapshot = parser().parse(fixture())
        val day = snapshot.day(LocalDate.of(2026, 11, 5))

        assertNotNull(day)
        assertEquals("post-392873", day!!.articleId)
        assertEquals("21:25", day.sourceUpdatedAt.toString())
    }

    @Test
    fun doesNotTurnForecastTextIntoTomorrowSchedule() {
        val snapshot = parser().parse(fixture())

        assertFalse(snapshot.days.any { it.date == LocalDate.of(2026, 11, 8) })
    }

    @Test
    fun acceptsTheSourceDashVariantsAndKeepsMissingQueuesMissing() {
        val html = """
            <article id="post-test">
              <h2>9 ЛИСТОПАДА ПО ЗАПОРІЗЬКІЙ ОБЛАСТІ ДІЯТИМУТЬ ГПВ</h2>
              <div class="content"><p>1.1: 00:00 – 00:30</p><p>3.2: 23:30 — 24:00</p></div>
            </article>
        """.trimIndent()

        val snapshot = parser().parse(html)
        val day = snapshot.day(LocalDate.of(2026, 11, 9))

        assertTrue(day!!.queues.containsKey("1.1"))
        assertEquals(30, day.queues["1.1"]!!.single().durationMinutes)
        assertEquals(1440, day.queues["3.2"]!!.single().endMinute)
        assertFalse(day.queues.containsKey("2.1"))
    }

    private fun parser() = ZoeOutageParser(clock)

    private fun fixture(): String =
        requireNotNull(javaClass.getResourceAsStream("/zoe-outage.html")) {
            "Fixture zoe-outage.html is missing"
        }.bufferedReader().use { it.readText() }
}
