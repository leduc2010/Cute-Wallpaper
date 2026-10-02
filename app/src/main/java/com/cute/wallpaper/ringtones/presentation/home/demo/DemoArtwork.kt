package com.cute.wallpaper.ringtones.presentation.home.demo

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.data.fake.FakeArtworkRegion

/** Renders reference artwork for the offline UI preview, without editing the source image. */
class DemoArtwork(resources: Resources, regions: List<FakeArtworkRegion>) {
    private val bitmap = BitmapFactory.decodeResource(resources, R.drawable.demo_wallpaper_reference)
    private val regions = regions.map { Rect(it.left, it.top, it.right, it.bottom) }

    fun drawable(index: Int): Drawable = RegionDrawable(bitmap, regions[index])

    private class RegionDrawable(private val bitmap: Bitmap, private val region: Rect) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        override fun draw(canvas: Canvas) { canvas.drawBitmap(bitmap, region, bounds, paint) }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
        override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
        @Deprecated("Deprecated in Android")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        override fun getIntrinsicWidth(): Int = region.width()
        override fun getIntrinsicHeight(): Int = region.height()
    }
}
