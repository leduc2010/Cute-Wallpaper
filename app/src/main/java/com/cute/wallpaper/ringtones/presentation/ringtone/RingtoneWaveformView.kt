package com.cute.wallpaper.ringtones.presentation.ringtone

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.cute.wallpaper.ringtones.R
import kotlin.math.ceil

class RingtoneWaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barHeights = floatArrayOf(
        0.30f, 0.60f, 0.90f, 0.50f, 0.70f, 0.30f,
        0.60f, 0.40f, 0.80f, 0.50f, 0.70f, 0.20f
    )
    private val activeColor = ContextCompat.getColor(context, R.color.tertiary_50)
    private val inactiveColor = ContextCompat.getColor(context, R.color.alpha_light_40)

    var progress: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (field == clamped) return
            field = clamped
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val barCount = barHeights.size
        val gap = dp(1.6f)
        val availableWidth = width - gap * (barCount - 1)
        val barWidth = availableWidth / barCount
        val radius = dp(1.2f)
        val activeBars = ceil(progress * barCount).toInt()

        barHeights.forEachIndexed { index, ratio ->
            val barHeight = height * ratio
            val left = index * (barWidth + gap)
            val top = (height - barHeight) / 2f
            paint.color = if (index < activeBars) activeColor else inactiveColor
            canvas.drawRoundRect(
                left,
                top,
                left + barWidth,
                top + barHeight,
                radius,
                radius,
                paint
            )
        }
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density
}
