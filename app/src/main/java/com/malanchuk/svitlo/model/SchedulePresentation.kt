package com.malanchuk.svitlo.model

/**
 * Applies the optional user-facing correction without changing the source schedule.
 *
 * The correction makes an outage start 30 minutes later and end 30 minutes earlier.
 * Intervals that collapse after the correction are omitted from the presentation.
 */
fun List<OutageInterval>.forDisplay(correctionEnabled: Boolean): List<OutageInterval> =
    if (!correctionEnabled) this else mapNotNull(OutageInterval::correctedByThirtyMinutes)

private fun OutageInterval.correctedByThirtyMinutes(): OutageInterval? {
    val correctedStart = startMinute + 30
    val correctedEnd = endMinute - 30
    return if (correctedStart < correctedEnd && correctedStart < 1440) {
        OutageInterval(correctedStart, correctedEnd)
    } else {
        null
    }
}
