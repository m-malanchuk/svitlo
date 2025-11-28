package com.malanchuk.svitlo.data

import com.malanchuk.svitlo.model.DailySchedule
import com.malanchuk.svitlo.model.OutageInterval
import com.malanchuk.svitlo.model.ScheduleSnapshot
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Compact process-independent cache format for the already parsed schedule. */
object ScheduleSnapshotCodec {
    private const val MAGIC = 0x53564C54 // SVLT
    private const val VERSION = 1
    private const val MAX_COLLECTION_SIZE = 10_000
    private const val MAX_STRING_BYTES = 1_000_000

    fun encode(snapshot: ScheduleSnapshot): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeLong(snapshot.fetchedAt.toEpochMilli())
            writeString(output, snapshot.sourceUrl)
            output.writeInt(snapshot.days.size)
            snapshot.days.forEach { day ->
                output.writeLong(day.date.toEpochDay())
                writeString(output, day.articleId)
                output.writeInt(day.sourceUpdatedAt?.toSecondOfDay() ?: -1)
                output.writeInt(day.queues.size)
                day.queues.forEach { (queue, intervals) ->
                    writeString(output, queue)
                    output.writeInt(intervals.size)
                    intervals.forEach { interval ->
                        output.writeInt(interval.startMinute)
                        output.writeInt(interval.endMinute)
                    }
                }
            }
        }
        bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): ScheduleSnapshot = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == MAGIC) { "Unknown schedule cache format" }
        require(input.readInt() == VERSION) { "Unsupported schedule cache version" }
        val fetchedAt = Instant.ofEpochMilli(input.readLong())
        val sourceUrl = readString(input)
        val days = List(readCollectionSize(input)) {
            val date = LocalDate.ofEpochDay(input.readLong())
            val articleId = readString(input)
            val sourceUpdatedSeconds = input.readInt()
            val queues = buildMap {
                repeat(readCollectionSize(input)) {
                    val queue = readString(input)
                    val intervals = List(readCollectionSize(input)) {
                        OutageInterval(input.readInt(), input.readInt())
                    }
                    put(queue, intervals)
                }
            }
            DailySchedule(
                date = date,
                queues = queues,
                articleId = articleId,
                sourceUpdatedAt = sourceUpdatedSeconds.takeIf { it >= 0 }
                    ?.toLong()
                    ?.let(LocalTime::ofSecondOfDay),
            )
        }
        ScheduleSnapshot(days = days, fetchedAt = fetchedAt, sourceUrl = sourceUrl)
    }

    private fun writeString(output: DataOutputStream, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_STRING_BYTES) { "Schedule cache string is too large" }
        output.writeInt(bytes.size)
        output.write(bytes)
    }

    private fun readString(input: DataInputStream): String {
        val size = input.readInt()
        require(size in 0..MAX_STRING_BYTES) { "Invalid schedule cache string size: $size" }
        val bytes = ByteArray(size)
        input.readFully(bytes)
        return bytes.toString(Charsets.UTF_8)
    }

    private fun readCollectionSize(input: DataInputStream): Int = input.readInt().also {
        require(it in 0..MAX_COLLECTION_SIZE) { "Invalid schedule cache collection size: $it" }
    }
}
