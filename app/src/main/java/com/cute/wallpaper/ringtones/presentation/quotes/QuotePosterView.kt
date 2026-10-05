package com.cute.wallpaper.ringtones.presentation.quotes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.cute.wallpaper.ringtones.R

/** Owns the poster artwork for both preview and exported images; controls are separate views. */
class QuotePosterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val textPaint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.tertiary_900)
        typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
    }
    private val markPaint = TextPaint(textPaint).apply {
        color = ContextCompat.getColor(context, R.color.alpha_primary_40)
        textAlign = android.graphics.Paint.Align.CENTER
    }
    private var quote = ""
    private var quoteLayout: StaticLayout? = null
    private var textLeft = 0f
    private var textTop = 0f
    private var markBaseline = 0f

    fun setQuote(value: String) {
        if (quote == value) return
        quote = value
        contentDescription = value
        rebuildLayout()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildLayout()
    }

    private fun rebuildLayout() {
        if (width <= 0 || height <= 0 || quote.isBlank()) { quoteLayout = null; return }
        val density = resources.displayMetrics.density
        val metrics = resources.displayMetrics
        val availableWidth = (width - 48 * density).toInt().coerceAtLeast(1)
        val availableHeight = height * .46f
        val initialSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 28f, metrics)
        val initialLineHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 36f, metrics)
        var size = initialSize
        var layout: StaticLayout
        do {
            textPaint.textSize = size
            layout = StaticLayout.Builder.obtain(quote, 0, quote.length, textPaint, availableWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .setLineSpacing(initialLineHeight * size / initialSize -
                    (textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent), 1f)
                .build()
            if (layout.height <= availableHeight || size <= 1f) break
            size *= .94f
        } while (true)
        quoteLayout = layout
        textLeft = (width - availableWidth) / 2f
        markPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 40f, metrics)
        val markHeight = markPaint.textSize
        val gap = 16 * density
        val top = height * .47f - (markHeight + gap + layout.height) / 2f
        markBaseline = top - markPaint.fontMetrics.ascent
        textTop = top + markHeight + gap
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(ContextCompat.getColor(context, R.color.primary_50))
        val layout = quoteLayout ?: return
        canvas.drawText(context.getString(R.string.quotes_mark), width / 2f, markBaseline, markPaint)
        canvas.save()
        canvas.translate(textLeft, textTop)
        layout.draw(canvas)
        canvas.restore()
    }

    fun snapshot(): Bitmap? {
        if (width <= 0 || height <= 0 || quoteLayout == null) return null
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { draw(Canvas(it)) }
    }
}
