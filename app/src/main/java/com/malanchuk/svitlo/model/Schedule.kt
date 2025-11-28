package com.malanchuk.svitlo.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** An exact wall-clock interval represented as minutes from local midnight. */
data class OutageInterval(
    val startMinute: Int,
    val endMinute: Int,
) {
    init {
        require(startMinute in 0..1439) { "Start must be within a day: $startMinute" }
        require(endMinute in 1..1440) { "End must be within a day: $endMinute" }
        require(startMinute < endMinute) { "An outage interval must have positive duration" }
    }

    val durationMinutes: Int get() = endMinute - startMinute

    fun contains(minuteOfDay: Int): Boolean = minuteOfDay in startMinute until endMinute

    fun startTime(): LocalTime = LocalTime.of(startMinute / 60, startMinute % 60)

    fun endTime(): LocalTime = if (endMinute == 1440) LocalTime.MIDNIGHT else {
        LocalTime.of(endMinute / 60, endMinute % 60)
    }
}

data class DailySchedule(
    val date: LocalDate,
    val queues: Map<String, List<OutageInterval>>,
    val articleId: String,
    val sourceUpdatedAt: LocalTime?,
)

data class ScheduleSnapshot(
    val days: List<DailySchedule>,
    val fetchedAt: Instant,
    val sourceUrl: String,
) {
    fun day(date: LocalDate): DailySchedule? = days.firstOrNull { it.date == date }

    fun queueNames(): List<String> = buildSet {
        addAll(QueueIds.default)
        days.flatMapTo(this) { it.queues.keys }
    }.sortedWith(compareBy<String> { it.substringBefore('.').toIntOrNull() ?: Int.MAX_VALUE }
        .thenBy { it.substringAfter('.').toIntOrNull() ?: Int.MAX_VALUE })

    fun digestFor(queue: String): String = days
        .sortedBy { it.date }
        .joinToString("|") { day ->
            val intervals = day.queues[queue].orEmpty()
                .joinToString(",") { "${it.startMinute}-${it.endMinute}" }
            "${day.date}:$intervals"
        }
}

object QueueIds {
    val default: List<String> = (1..6).flatMap { group -> listOf("$group.1", "$group.2") }
}
