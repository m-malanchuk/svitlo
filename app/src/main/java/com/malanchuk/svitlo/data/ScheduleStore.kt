package com.malanchuk.svitlo.data

import android.content.Context
import com.malanchuk.svitlo.model.DailySchedule
import com.malanchuk.svitlo.model.OutageInterval
import com.malanchuk.svitlo.model.ScheduleSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class ScheduleStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val snapshotFile = File(appContext.filesDir, SNAPSHOT_FILE)

    fun load(): ScheduleSnapshot? {
        val binarySnapshot = runCatching {
            if (snapshotFile.exists()) ScheduleSnapshotCodec.decode(snapshotFile.readBytes()) else null
        }.getOrNull()
        if (binarySnapshot != null) return binarySnapshot

        val legacySnapshot = runCatching {
            val raw = preferences.getString(KEY_SNAPSHOT, null) ?: return@runCatching null
            decodeLegacy(raw)
        }.getOrNull()
        if (legacySnapshot != null) writeBinarySnapshot(legacySnapshot)
        return legacySnapshot
    }

    fun loadFast(): ScheduleSnapshot? = runCatching {
        if (snapshotFile.exists()) ScheduleSnapshotCodec.decode(snapshotFile.readBytes()) else null
    }.getOrNull()

    fun hasFastSnapshot(): Boolean = snapshotFile.exists()

    fun save(snapshot: ScheduleSnapshot) {
        writeBinarySnapshot(snapshot)
        preferences.edit().putString(KEY_SNAPSHOT, encode(snapshot)).apply()
    }

    fun selectedQueue(): String = preferences.getString(KEY_QUEUE, null) ?: "1.1"

    fun setSelectedQueue(queue: String) {
        preferences.edit().putString(KEY_QUEUE, queue).apply()
    }

    fun languageTag(): String = preferences.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

    fun setLanguageTag(languageTag: String) {
        preferences.edit().putString(KEY_LANGUAGE, languageTag).apply()
    }

    fun correction30MinutesEnabled(): Boolean = preferences.getBoolean(KEY_CORRECTION_30_MINUTES, false)

    fun setCorrection30MinutesEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_CORRECTION_30_MINUTES, enabled).apply()
    }

    fun refreshIntervalMinutes(): Int = preferences.getInt(KEY_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL)

    fun setRefreshIntervalMinutes(minutes: Int) {
        preferences.edit().putInt(KEY_REFRESH_INTERVAL, minutes).apply()
    }

    fun lastNotifiedDigest(): String? = preferences.getString(KEY_LAST_NOTIFIED, null)

    fun setLastNotifiedDigest(digest: String) {
        preferences.edit().putString(KEY_LAST_NOTIFIED, digest).apply()
    }

    private fun encode(snapshot: ScheduleSnapshot): String = JSONObject()
        .put("fetchedAt", snapshot.fetchedAt.toString())
        .put("sourceUrl", snapshot.sourceUrl)
        .put("days", JSONArray().apply {
            snapshot.days.forEach { day ->
                put(JSONObject().apply {
                    put("date", day.date.toString())
                    put("articleId", day.articleId)
                    put("sourceUpdatedAt", day.sourceUpdatedAt?.toString() ?: JSONObject.NULL)
                    put("queues", JSONObject().apply {
                        day.queues.forEach { (queue, intervals) ->
                            put(queue, JSONArray().apply {
                                intervals.forEach { interval ->
                                    put(JSONArray().put(interval.startMinute).put(interval.endMinute))
                                }
                            })
                        }
                    })
                })
            }
        }).toString()

    private fun decodeLegacy(raw: String): ScheduleSnapshot {
        val root = JSONObject(raw)
        val days = buildList {
            val daysJson = root.getJSONArray("days")
            repeat(daysJson.length()) { dayIndex ->
                val dayJson = daysJson.getJSONObject(dayIndex)
                val queuesJson = dayJson.getJSONObject("queues")
                val queues = buildMap {
                    queuesJson.keys().forEach { queue ->
                        val intervalsJson = queuesJson.getJSONArray(queue)
                        put(queue, buildList {
                            repeat(intervalsJson.length()) { intervalIndex ->
                                val pair = intervalsJson.getJSONArray(intervalIndex)
                                add(OutageInterval(pair.getInt(0), pair.getInt(1)))
                            }
                        })
                    }
                }
                add(
                    DailySchedule(
                        date = LocalDate.parse(dayJson.getString("date")),
                        queues = queues,
                        articleId = dayJson.getString("articleId"),
                        sourceUpdatedAt = dayJson.optString("sourceUpdatedAt")
                            .takeUnless { it.isBlank() || it == "null" }
                            ?.let(LocalTime::parse),
                    ),
                )
            }
        }
        return ScheduleSnapshot(
            days = days,
            fetchedAt = Instant.parse(root.getString("fetchedAt")),
            sourceUrl = root.getString("sourceUrl"),
        )
    }

    private fun writeBinarySnapshot(snapshot: ScheduleSnapshot) {
        runCatching {
            val tempFile = File.createTempFile("schedule_snapshot", ".tmp", appContext.filesDir)
            tempFile.writeBytes(ScheduleSnapshotCodec.encode(snapshot))
            if (!tempFile.renameTo(snapshotFile)) {
                snapshotFile.writeBytes(tempFile.readBytes())
                tempFile.delete()
            }
        }
    }

    private companion object {
        const val PREFERENCES = "svitlo_preferences"
        const val KEY_SNAPSHOT = "schedule_snapshot"
        const val SNAPSHOT_FILE = "schedule_snapshot.bin"
        const val KEY_QUEUE = "selected_queue"
        const val KEY_LANGUAGE = "language_tag"
        const val KEY_CORRECTION_30_MINUTES = "correction_30_minutes"
        const val KEY_REFRESH_INTERVAL = "refresh_interval_minutes"
        const val KEY_LAST_NOTIFIED = "last_notified_digest"
        const val DEFAULT_LANGUAGE = "uk"
        const val DEFAULT_REFRESH_INTERVAL = 15
    }
}
