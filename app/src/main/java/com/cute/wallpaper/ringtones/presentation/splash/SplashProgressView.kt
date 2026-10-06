package com.cute.wallpaper.ringtones.presentation.splash

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.cute.wallpaper.ringtones.R

class SplashProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.neutral_0)
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary_500)
    }
    private val rect = RectF()
    private var progressValue = 0f

    init {
        if (isInEditMode) {
            progressValue = 0.85f
        }
    }

    var progress: Float
        get() = progressValue
        set(value) {
            progressValue = value.coerceIn(0f, 1f)
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radius = height / 2f
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, trackPaint)

        val progressWidth = width * progressValue
        if (progressWidth <= 0f) return

        rect.set(0f, 0f, progressWidth, height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, progressPaint)
    }
}
