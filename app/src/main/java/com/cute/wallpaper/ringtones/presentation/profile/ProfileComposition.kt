package com.cute.wallpaper.ringtones.presentation.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.max
import kotlin.math.min

enum class ProfileAdjustment { ORIGINAL, MONO, WARM, COOL }
enum class ProfileFrame { NONE, PINK, PURPLE, SPARKLE }

/** Original Figma decorations shared by the preview and its immutable export snapshot. */
data class ProfileCompositionIcons(
    val sparkle24: Bitmap,
    val sparkle32: Bitmap,
    val sparkle20: Bitmap,
    val sparkle16: Bitmap,
    val nameMagic: Bitmap
)

/** A snapshot for preview/export; the state owner retains an immutable source bitmap. */
data class ProfileComposition(
    val bitmap: Bitmap,
    val transform: ProfilePhotoTransform = ProfilePhotoTransform(),
    val name: String = "",
    val adjustment: ProfileAdjustment = ProfileAdjustment.ORIGINAL,
    val frame: ProfileFrame = ProfileFrame.NONE,
    val backgroundColor: Int = Color.rgb(225, 202, 234),
    val nameTypeface: Typeface = Typeface.create("sans-serif", Typeface.BOLD),
    val icons: ProfileCompositionIcons? = null
) {
    fun renderBitmap(size: Int = 1024): Bitmap {
        require(size in 1..2048)
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        try {
            draw(Canvas(result), RectF(0f, 0f, size.toFloat(), size.toFloat()))
            return result
        } catch (failure: Throwable) {
            result.recycle()
            throw failure
        }
    }

    fun draw(canvas: Canvas, bounds: RectF, drawSparkles: Boolean = true) {
        val edge = min(bounds.width(), bounds.height())
        if (edge <= 0f || bitmap.isRecycled) return
        val left = bounds.centerX() - edge / 2f
        val top = bounds.centerY() - edge / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val square = RectF(left, top, left + edge, top + edge)
        paint.color = backgroundColor
        canvas.drawRoundRect(square, edge * 36f / 340f, edge * 36f / 340f, paint)
        val borderWidth = edge / 340f
        val border = RectF(square).apply { inset(borderWidth / 2f, borderWidth / 2f) }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderWidth
        paint.color = Color.rgb(251, 220, 232)
        canvas.drawRoundRect(border, edge * 36f / 340f - borderWidth / 2f,
            edge * 36f / 340f - borderWidth / 2f, paint)
        paint.style = Paint.Style.FILL

        val diameter = edge * 300f / 340f
        val radius = diameter / 2f
        val centerX = square.centerX()
        val centerY = square.centerY()
        drawPhoto(canvas, RectF(centerX - radius, centerY - radius,
            centerX + radius, centerY + radius), radius)

        when (frame) {
            ProfileFrame.NONE, ProfileFrame.SPARKLE -> Unit
            ProfileFrame.PINK, ProfileFrame.PURPLE -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = edge * 6f / 340f
                paint.color = if (frame == ProfileFrame.PINK) Color.rgb(236, 91, 140)
                else Color.rgb(169, 133, 229)
                canvas.drawCircle(centerX, centerY, radius, paint)
                paint.style = Paint.Style.FILL
            }
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = edge * 2f / 340f
        paint.color = Color.WHITE
        canvas.drawCircle(centerX, centerY, radius - paint.strokeWidth / 2f, paint)
        paint.style = Paint.Style.FILL
        icons?.takeIf { drawSparkles }?.let { assets ->
            drawIcon(canvas, assets.sparkle24, left + edge * 71f / 340f, top + edge * 26f / 340f, edge * 24f / 340f)
            drawIcon(canvas, assets.sparkle32, left + edge * 285f / 340f, top + edge * 95f / 340f, edge * 32f / 340f)
            drawIcon(canvas, assets.sparkle20, left + edge * 29f / 340f, top + edge * 231f / 340f, edge * 20f / 340f)
            drawIcon(canvas, assets.sparkle16, left + edge * 313f / 340f, top + edge * 213f / 340f, edge * 16f / 340f)
        }
        if (name.isNotBlank()) drawName(canvas, square, name, paint)
    }

    /** Preset slots show only the photo, with the same pan, zoom and adjustment as the editor. */
    internal fun drawPhoto(canvas: Canvas, bounds: RectF, cornerRadius: Float) {
        val diameter = min(bounds.width(), bounds.height())
        if (diameter <= 0f || bitmap.isRecycled) return
        val safe = transform.constrained(bitmap.width, bitmap.height)
        val scale = max(diameter / bitmap.width, diameter / bitmap.height) * safe.zoom
        val imageWidth = bitmap.width * scale
        val imageHeight = bitmap.height * scale
        val imageRect = RectF(
            bounds.centerX() - imageWidth / 2f + safe.offsetX * diameter,
            bounds.centerY() - imageHeight / 2f + safe.offsetY * diameter,
            bounds.centerX() + imageWidth / 2f + safe.offsetX * diameter,
            bounds.centerY() + imageHeight / 2f + safe.offsetY * diameter
        )
        val clip = Path().apply { addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW) }
        val checkpoint = canvas.save()
        canvas.clipPath(clip)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = adjustment.colorFilter()
        }
        canvas.drawBitmap(bitmap, null, imageRect, paint)
        canvas.restoreToCount(checkpoint)
    }

    private fun drawName(canvas: Canvas, square: RectF, text: String, paint: Paint) {
        val unit = square.width() / 340f
        val pill = RectF(square.left + 73f * unit, square.top + 280f * unit,
            square.left + 267f * unit, square.top + 322f * unit)
        paint.shader = LinearGradient(pill.left, pill.centerY(), pill.right, pill.centerY(),
            Color.rgb(181, 155, 239), Color.rgb(236, 91, 140), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(pill, 20f * unit, 20f * unit, paint)
        paint.shader = null
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = unit
        canvas.drawRoundRect(pill, 20f * unit, 20f * unit, paint)
        paint.style = Paint.Style.FILL
        paint.typeface = nameTypeface
        paint.textSize = 16f * unit
        paint.textAlign = Paint.Align.CENTER
        val available = 98f * unit
        var display = text.replace('\n', ' ')
        if (paint.measureText(display) > available) {
            val count = paint.breakText(display, true, available - paint.measureText("…"), null)
            display = display.take(count) + "…"
        }
        canvas.drawText(display, pill.centerX(), pill.centerY() - (paint.ascent() + paint.descent()) / 2f, paint)
        icons?.let { assets ->
            drawIcon(canvas, assets.nameMagic, pill.left + 32f * unit, pill.centerY(), 16f * unit)
            drawIcon(canvas, assets.nameMagic, pill.right - 32f * unit, pill.centerY(), 16f * unit)
        }
    }

    private fun drawIcon(canvas: Canvas, icon: Bitmap, x: Float, y: Float, size: Float) {
        val half = size / 2f
        val destination = RectF(x - half, y - half, x + half, y + half)
        canvas.drawBitmap(icon, null, destination, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    private fun ProfileAdjustment.colorFilter(): ColorMatrixColorFilter? = when (this) {
        ProfileAdjustment.ORIGINAL -> null
        ProfileAdjustment.MONO -> ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        ProfileAdjustment.WARM -> ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            1.08f, 0f, 0f, 0f, 5f, 0f, 1f, 0f, 0f, 2f,
            0f, 0f, .9f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
        ProfileAdjustment.COOL -> ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            .92f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 2f,
            0f, 0f, 1.08f, 0f, 5f, 0f, 0f, 0f, 1f, 0f)))
    }
}
