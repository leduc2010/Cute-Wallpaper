package com.cute.wallpaper.ringtones.domain.model

data class AutoChangeConfig(
    val enabled: Boolean,
    val target: WallpaperTarget,
    val intervalHours: Int
) {
    companion object {
        const val MIN_DOWNLOADED_WALLPAPERS = 5
        const val DEFAULT_INTERVAL_HOURS = 12

        val SUPPORTED_INTERVAL_HOURS = setOf(3, 6, 12, 24)
    }
}
