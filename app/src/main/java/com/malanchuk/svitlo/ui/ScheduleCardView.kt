package com.malanchuk.svitlo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.malanchuk.svitlo.R
import com.malanchuk.svitlo.model.ScheduleSnapshot
import com.malanchuk.svitlo.model.forDisplay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

/** Native schedule card used by both the main screen and the startup path. */
open class ScheduleCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity
    private val radius = 12f * density
    private val padding = 18f * density
    private val primary = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD }
    private val secondary = Paint(Paint.ANTI_ALIAS_FLAG)
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD }
    private val body = Paint(Paint.ANTI_ALIAS_FLAG)
    private val small = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cardRect = RectF()

    private var snapshot: ScheduleSnapshot? = null
    private var selectedDate: LocalDate = LocalDate.now()
    private var queue: String = "1.1"
    private var correctionEnabled = false
    private var languageTag = "uk"

    fun bind(
        snapshot: ScheduleSnapshot?,
        date: LocalDate = LocalDate.now(),
        queue: String = "1.1",
        correctionEnabled: Boolean = false,
        languageTag: String = "uk",
    ) {
        this.snapshot = snapshot
        this.selectedDate = date
        this.queue = queue
        this.correctionEnabled = correctionEnabled
        this.languageTag = languageTag
        invalidate()
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val intervals = snapshot?.day(selectedDate)?.queues?.get(queue).orEmpty().forDisplay(correctionEnabled)
        val desiredDp = if (snapshot?.day(selectedDate) == null) 210 else 247 + intervals.size * 36
        setMeasuredDimension(width, resolveSize((desiredDp * density).toInt(), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val textColor = color(R.color.schedule_text_primary)
        val secondaryColor = color(R.color.schedule_text_secondary)
        val powerColor = color(R.color.schedule_power)
        val outageColor = color(R.color.schedule_outage)

        cardRect.set(0f, 0f, width.toFloat(), height.toFloat())
        val gradientStart = color(R.color.schedule_gradient_start)
        val gradientEnd = color(R.color.schedule_gradient_end)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, height.toFloat(), gradientStart, gradientEnd, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(cardRect, radius, radius, cardPaint)

        val schedule = snapshot?.day(selectedDate)
        val intervals = schedule?.queues?.get(queue).orEmpty().forDisplay(correctionEnabled)
        title.textSize = sp(18f)
        small.textSize = sp(13f)
        val y = padding + title.textSize
        if (schedule == null) {
            primary.color = textColor
            primary.textSize = sp(22f)
            val icon = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
            val iconSize = dp(56f).toInt()
            val iconLeft = (width - iconSize) / 2
            val iconTop = dp(28f).toInt()
            icon?.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            icon?.draw(canvas)
            val missingTitle = context.getString(R.string.schedule_missing_title)
            canvas.drawText(missingTitle, (width - primary.measureText(missingTitle)) / 2f, dp(110f), primary)
            secondary.color = secondaryColor
            secondary.textSize = sp(16f)
            drawCenteredWrapped(
                canvas,
                context.getString(R.string.schedule_missing_body),
                dp(130f),
                width - padding * 2,
                secondary,
            )
            return
        }

        title.color = textColor
        canvas.drawText(formatDate(selectedDate), padding, y, title)
        secondary.color = secondaryColor
        secondary.textSize = sp(14f)
        canvas.drawText("${context.getString(R.string.queue_label)} $queue", padding, y + dp(22f), secondary)

        val isToday = selectedDate == LocalDate.now()
        val outageNow = isToday && intervals.any { it.contains(currentMinute()) }
        val statusText = when {
            !isToday -> context.getString(R.string.schedule_available)
            outageNow -> context.getString(R.string.current_outage)
            else -> context.getString(R.string.current_power)
        }
        val statusColor = if (outageNow) outageColor else powerColor
        small.typeface = android.graphics.Typeface.DEFAULT_BOLD
        small.color = statusColor
        val statusWidth = small.measureText(statusText) + dp(20f)
        val statusTop = padding + dp(9f)
        val statusLeft = width - padding - statusWidth
        canvas.drawRoundRect(RectF(statusLeft, statusTop, width - padding, statusTop + dp(28f)), dp(20f), dp(20f), fill(statusColor))
        small.color = color(R.color.startup_on_primary)
        canvas.drawText(statusText, statusLeft + dp(10f), statusTop + dp(18f), small)
        small.typeface = android.graphics.Typeface.DEFAULT

        val timelineTop = y + dp(48f)
        val timelineBottom = timelineTop + dp(38f)
        val timelineRect = RectF(padding, timelineTop, width - padding, timelineBottom)
        canvas.drawRoundRect(timelineRect, dp(11f), dp(11f), fill(powerColor))
        val timelineClip = Path().apply {
            addRoundRect(timelineRect, dp(11f), dp(11f), Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(timelineClip)
        intervals.forEach { interval ->
            val left = timelineRect.left + timelineRect.width() * interval.startMinute / 1440f
            val right = timelineRect.left + timelineRect.width() * interval.endMinute / 1440f
            canvas.drawRect(left, timelineRect.top, max(left + 1f, right), timelineRect.bottom, fill(outageColor))
        }
        if (isToday) {
            val x = timelineRect.left + timelineRect.width() * currentMinute().coerceIn(0, 1439) / 1440f
            canvas.drawRect(x, timelineRect.top, x + dp(3f), timelineRect.bottom, fill(android.graphics.Color.WHITE))
        }
        canvas.restore()
        secondary.color = secondaryColor
        secondary.textSize = sp(11f)
        listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEachIndexed { index, label ->
            val x = timelineRect.left + timelineRect.width() * index / 4f
            val offset = if (index == 4) secondary.measureText(label) else if (index > 0) secondary.measureText(label) / 2f else 0f
            canvas.drawText(label, x - offset, timelineBottom + dp(16f), secondary)
        }

        primary.color = textColor
        primary.textSize = sp(17f)
        val periodsY = timelineBottom + dp(48f)
        canvas.drawText(context.getString(R.string.outage_periods), padding, periodsY, primary)
        body.color = textColor
        body.textSize = sp(17f)
        if (intervals.isEmpty()) {
            secondary.color = secondaryColor
            secondary.textSize = sp(16f)
            canvas.drawText(context.getString(R.string.no_outages), padding, periodsY + dp(27f), secondary)
        } else {
            intervals.forEachIndexed { index, interval ->
                val rowY = periodsY + (index + 1) * dp(31f)
                canvas.drawCircle(padding + dp(4f), rowY - dp(5f), dp(4f), fill(outageColor))
                canvas.drawText(context.getString(R.string.outage_period, formatMinute(interval.startMinute), formatMinute(interval.endMinute)), padding + dp(16f), rowY, body)
                secondary.color = secondaryColor
                secondary.textSize = sp(14f)
                val duration = context.getString(R.string.duration_format, interval.durationMinutes / 60, interval.durationMinutes % 60)
                canvas.drawText(duration, width - padding - secondary.measureText(duration), rowY, secondary)
            }
        }

        snapshot?.fetchedAt?.let { fetchedAt ->
            secondary.color = secondaryColor
            secondary.textSize = sp(13f)
            val fetched = context.getString(R.string.fetched_at, fetchedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM HH:mm", Locale.forLanguageTag(languageTag))))
            canvas.drawText(fetched, padding, height - padding, secondary)
        }
    }

    private fun drawWrapped(canvas: Canvas, text: String, x: Float, baseline: Float, maxWidth: Float, paint: Paint) {
        var line = ""
        var currentBaseline = baseline
        text.split(" ").forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line, x, currentBaseline, paint)
                currentBaseline += paint.textSize * 1.35f
                line = word
            } else line = candidate
        }
        if (line.isNotEmpty()) canvas.drawText(line, x, currentBaseline, paint)
    }

    private fun drawCenteredWrapped(canvas: Canvas, text: String, firstBaseline: Float, maxWidth: Float, paint: Paint) {
        val lines = mutableListOf<String>()
        var line = ""
        text.split(" ").forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                lines += line
                line = word
            } else {
                line = candidate
            }
        }
        if (line.isNotEmpty()) lines += line
        lines.forEachIndexed { index, value ->
            canvas.drawText(value, (width - paint.measureText(value)) / 2f, firstBaseline + index * paint.textSize * 1.35f, paint)
        }
    }

    private fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag(languageTag)))
    private fun formatMinute(minute: Int): String = if (minute == 1440) "24:00" else "%02d:%02d".format(Locale.ROOT, minute / 60, minute % 60)
    private fun currentMinute(): Int { val now = LocalDateTime.now(); return now.hour * 60 + now.minute }
    private fun dp(value: Float): Float = value * density
    private fun sp(value: Float): Float = value * scaledDensity
    private fun color(id: Int): Int = ContextCompat.getColor(context, id)
    private fun fill(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
}
