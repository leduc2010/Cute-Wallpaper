package com.cute.wallpaper.ringtones.presentation.home

enum class WallpaperCollection(val category: String) {
    WALLPAPER("wallpaper"),
    DUAL_WALLPAPERS("dual"),
    BEST("best");

    companion object {
        val pages = entries
    }
}
