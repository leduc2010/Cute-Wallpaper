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
    WALLPAPER(R.string.home_wallpaper_category, R.drawable.img_collection_wallpaper, 3),
    DUAL(R.string.favorite_category_dual, R.drawable.img_collection_dual_wallpapers, 3),
    VIDEO(R.string.favorite_category_video, R.drawable.ic_nav_video_selected, 3),
    RINGTONES(R.string.ringtones, R.drawable.ic_nav_ringtone_selected, 1),
    QUOTES(R.string.quotes, R.drawable.ic_nav_quotes_selected, 2),
    PROFILE(R.string.profile_picture_title, R.drawable.ic_nav_profile_selected, 2);

    fun matches(item: HomeContentUiModel): Boolean = when (this) {
        WALLPAPER -> item.type == ContentType.WALLPAPER && item.collection != WallpaperCollection.DUAL_WALLPAPERS
        DUAL -> item.type == ContentType.WALLPAPER && item.collection == WallpaperCollection.DUAL_WALLPAPERS
        VIDEO -> item.type == ContentType.VIDEO_WALLPAPER
        RINGTONES -> item.type == ContentType.RINGTONE
        QUOTES -> item.type == ContentType.QUOTE
        PROFILE -> item.type == ContentType.PROFILE_PICTURE
    }
}
