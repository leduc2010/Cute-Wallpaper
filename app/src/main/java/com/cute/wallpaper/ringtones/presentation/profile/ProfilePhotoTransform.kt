package com.cute.wallpaper.ringtones.presentation.profile

import kotlin.math.max

/** Offsets are fractions of the photo diameter, so the crop survives view size changes. */
data class ProfilePhotoTransform(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    fun constrained(imageWidth: Int, imageHeight: Int): ProfilePhotoTransform {
        if (imageWidth <= 0 || imageHeight <= 0) return ProfilePhotoTransform()
        val safeZoom = zoom.takeIf { it.isFinite() }?.coerceIn(1f, MAX_ZOOM) ?: 1f
        val width = max(1f, imageWidth.toFloat() / imageHeight) * safeZoom
        val height = max(1f, imageHeight.toFloat() / imageWidth) * safeZoom
        val limitX = (width - 1f) / 2f
        val limitY = (height - 1f) / 2f
        return ProfilePhotoTransform(
            safeZoom,
            (offsetX.takeIf { it.isFinite() } ?: 0f).coerceIn(-limitX, limitX),
            (offsetY.takeIf { it.isFinite() } ?: 0f).coerceIn(-limitY, limitY)
        )
    }

    fun translated(dx: Float, dy: Float, imageWidth: Int, imageHeight: Int) =
        copy(offsetX = offsetX + dx, offsetY = offsetY + dy)
            .constrained(imageWidth, imageHeight)

    /** Focus is relative to the circle center, in photo-diameter units. */
    fun zoomedAt(
        factor: Float,
        focusX: Float,
        focusY: Float,
        imageWidth: Int,
        imageHeight: Int
    ): ProfilePhotoTransform {
        val current = constrained(imageWidth, imageHeight)
        if (!factor.isFinite() || factor <= 0f) return current
        val newZoom = (current.zoom * factor).coerceIn(1f, MAX_ZOOM)
        val ratio = newZoom / current.zoom
        return ProfilePhotoTransform(
            newZoom,
            focusX - (focusX - current.offsetX) * ratio,
            focusY - (focusY - current.offsetY) * ratio
        ).constrained(imageWidth, imageHeight)
    }

    companion object {
        const val MAX_ZOOM = 6f
    }
}
