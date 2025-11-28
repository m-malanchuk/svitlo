package com.malanchuk.svitlo.parser

import com.malanchuk.svitlo.model.DailySchedule
import com.malanchuk.svitlo.model.OutageInterval
import com.malanchuk.svitlo.model.ScheduleSnapshot
import org.jsoup.Jsoup
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.math.abs

/**
 * Parses the public outage archive at zoe.com.ua.
 *
 * The page is an archive where newer updates are listed first. For each calendar
 * date, the latest update is selected; an update may contain only a subset of
 * queues, which is still a complete revision for that date in the source format.
 */
class ZoeOutageParser(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun parse(
        html: String,
        fetchedAt: Instant = Instant.now(clock),
        sourceUrl: String = SOURCE_URL,
    ): ScheduleSnapshot {
        val document = Jsoup.parse(html)
        val candidates = linkedMapOf<LocalDate, MutableList<ArticleCandidate>>()

        document.select("article[id^=post-]").forEachIndexed { order, article ->
            val title = article.selectFirst("h2")?.text().orEmpty()
            val date = parseDate(title) ?: return@forEachIndexed
            val content = article.selectFirst(".content")?.text().orEmpty()
            val queues = parseQueues(content)
            if (queues.isEmpty()) return@forEachIndexed

            val updatedAt = UPDATE_TIME.find(title.lowercase(Locale.ROOT))?.let { match ->
                parseTime(match.groupValues[1], allowMidnight = false)
            }
            val articleId = article.id().ifBlank { "unknown-$order" }
            candidates.getOrPut(date) { mutableListOf() }
                .add(ArticleCandidate(date, queues, articleId, updatedAt, order))
        }

        val days = candidates.mapNotNull { (_, revisions) ->
            revisions.maxWithOrNull(
                compareBy<ArticleCandidate> { it.updatedAt?.toSecondOfDay() ?: -1 }
                    .thenBy { -it.order },
            )?.let { latest ->
                DailySchedule(
                    date = latest.date,
                    queues = latest.queues,
                    articleId = latest.articleId,
                    sourceUpdatedAt = latest.updatedAt,
                )
            }
        }.sortedBy { it.date }

        return ScheduleSnapshot(
            days = days,
            fetchedAt = fetchedAt,
            sourceUrl = sourceUrl,
        )
    }

    private fun parseDate(title: String): LocalDate? {
        val match = DATE.find(title.lowercase(Locale.ROOT)) ?: return null
        val day = match.groupValues[1].toIntOrNull() ?: return null
        val month = MONTHS[match.groupValues[2].lowercase(Locale.ROOT)] ?: return null
        val today = LocalDate.now(clock)

        return listOf(today.year - 1, today.year, today.year + 1)
            .mapNotNull { year -> runCatching { LocalDate.of(year, month, day) }.getOrNull() }
            .minByOrNull { candidate -> abs(candidate.toEpochDay() - today.toEpochDay()) }
    }

    private fun parseQueues(content: String): Map<String, List<OutageInterval>> {
        val result = linkedMapOf<String, List<OutageInterval>>()
        QUEUE_BLOCK.findAll(content).forEach { match ->
            val queue = match.groupValues[1]
            val intervals = INTERVAL.findAll(match.groupValues[2]).mapNotNull { interval ->
                val start = parseTime(interval.groupValues[1], allowMidnight = true) ?: return@mapNotNull null
                val end = parseTime(interval.groupValues[2], allowMidnight = true) ?: return@mapNotNull null
                val startMinute = start.hour * 60 + start.minute
                val endMinute = if (end == LocalTime.MIDNIGHT && interval.groupValues[2].startsWith("24:")) {
                    1440
                } else {
                    end.hour * 60 + end.minute
                }
                runCatching { OutageInterval(startMinute, endMinute) }.getOrNull()
            }.toList()
            if (intervals.isNotEmpty()) result[queue] = intervals
        }
        return result
    }

    private fun parseTime(value: String, allowMidnight: Boolean): LocalTime? {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        if (minute !in 0..59) return null
        if (hour == 24 && allowMidnight && minute == 0) return LocalTime.MIDNIGHT
        if (hour !in 0..23) return null
        if (!allowMidnight && hour == 24) return null
        return LocalTime.of(hour, minute)
    }

    private data class ArticleCandidate(
        val date: LocalDate,
        val queues: Map<String, List<OutageInterval>>,
        val articleId: String,
        val updatedAt: LocalTime?,
        val order: Int,
    )

    private companion object {
        const val SOURCE_URL = "https://www.zoe.com.ua/outage/"

        val DATE = Regex(
            "(?<!\\d)(\\d{1,2})\\s+(січня|лютого|березня|квітня|травня|червня|липня|серпня|вересня|жовтня|листопада|грудня)(?![\\p{L}])",
        )
        val UPDATE_TIME = Regex("оновлено\\s+(\\d{1,2}:\\d{2})")
        val QUEUE_BLOCK = Regex(
            "(?s)(?<!\\d)(\\d{1,2}\\.\\d)\\s*:\\s*(.*?)(?=\\s+\\d{1,2}\\.\\d\\s*:|$)",
        )
        val INTERVAL = Regex("(\\d{1,2}:\\d{2})\\s*[-–—]\\s*(\\d{1,2}:\\d{2})")
        val MONTHS = mapOf(
            "січня" to 1,
            "лютого" to 2,
            "березня" to 3,
            "квітня" to 4,
            "травня" to 5,
            "червня" to 6,
            "липня" to 7,
            "серпня" to 8,
            "вересня" to 9,
            "жовтня" to 10,
            "листопада" to 11,
            "грудня" to 12,
        )
    }
}
