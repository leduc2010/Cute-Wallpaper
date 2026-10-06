package com.cute.wallpaper.ringtones.presentation.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.cute.wallpaper.ringtones.R

enum class WallpaperCollection(
    val category: String,
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int
) {
    WALLPAPER(
        "wallpaper",
        R.string.home_wallpaper_category,
        R.drawable.img_collection_wallpaper
    ),
    DUAL_WALLPAPERS(
        "dual",
        R.string.home_dual_wallpapers_category,
        R.drawable.img_collection_dual_wallpapers
    ),
    BEST(
        "best",
        R.string.home_best_category,
        R.drawable.img_collection_best
    );

    companion object {
        val pages: List<WallpaperCollection> = entries
    }
}
