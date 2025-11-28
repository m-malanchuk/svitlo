package com.malanchuk.svitlo.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulePresentationTest {
    @Test
    fun correctionMovesOutageStartLaterAndPowerReturnEarlier() {
        val corrected = listOf(OutageInterval(13 * 60, 18 * 60)).forDisplay(correctionEnabled = true)

        assertEquals(listOf(13 * 60 + 30 to 17 * 60 + 30), corrected.map { it.startMinute to it.endMinute })
    }

    @Test
    fun correctionDoesNotChangeStoredIntervalsWhenDisabled() {
        val original = listOf(OutageInterval(13 * 60, 18 * 60))

        assertEquals(original, original.forDisplay(correctionEnabled = false))
    }

    @Test
    fun correctionDropsIntervalsThatCollapse() {
        val corrected = listOf(OutageInterval(13 * 60, 13 * 60 + 30)).forDisplay(correctionEnabled = true)

        assertTrue(corrected.isEmpty())
    }
}
