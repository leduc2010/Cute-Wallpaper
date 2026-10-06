package com.cute.wallpaper.ringtones.presentation.favorites

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.WallpaperCollection

enum class FavoriteCategory(
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int,
    val columns: Int
) {
    WALLPAPER(R.string.home_wallpaper_category, R.drawable.ic_favorite_tab_wallpapers, 3),
    DUAL(R.string.favorite_category_dual, R.drawable.ic_favorite_tab_dual_wallpapers, 3),
    VIDEO(R.string.favorite_category_video, R.drawable.ic_favorite_tab_video, 3),
    RINGTONES(R.string.ringtones, R.drawable.ic_favorite_tab_ringtones, 1),
    QUOTES(R.string.quotes, R.drawable.ic_ringtone_tab_notifications, 2);

    fun matches(item: HomeContentUiModel): Boolean = when (this) {
        WALLPAPER -> item.type == ContentType.WALLPAPER && item.collection != WallpaperCollection.DUAL_WALLPAPERS
        DUAL -> item.type == ContentType.WALLPAPER && item.collection == WallpaperCollection.DUAL_WALLPAPERS
        VIDEO -> item.type == ContentType.VIDEO_WALLPAPER
        RINGTONES -> item.type == ContentType.RINGTONE
        QUOTES -> item.type == ContentType.QUOTE
    }
}
