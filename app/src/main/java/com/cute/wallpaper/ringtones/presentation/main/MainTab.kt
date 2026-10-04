package com.cute.wallpaper.ringtones.presentation.main

import androidx.annotation.StringRes
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.domain.model.ContentType

enum class MainTab(
    @param:StringRes val titleRes: Int,
    @param:StringRes val searchHintRes: Int,
    val contentType: ContentType?
) {
    WALLPAPERS(
        R.string.wallpapers,
        R.string.search_wallpapers,
        ContentType.WALLPAPER
    ),
    VIDEO_WALLPAPERS(
        R.string.video_wallpapers,
        R.string.search_video_wallpapers,
        ContentType.VIDEO_WALLPAPER
    ),
    RINGTONES(
        R.string.ringtones,
        R.string.search_ringtones,
        ContentType.RINGTONE
    ),
    PROFILE_PICTURES(
        R.string.profile_pictures,
        R.string.search_profile_pictures,
        ContentType.PROFILE_PICTURE
    ),
    QUOTES(
        R.string.quotes,
        R.string.search_quotes,
        ContentType.QUOTE
    ),
    FAVORITES(
        R.string.favorites,
        R.string.search_favorites,
        null
    )
}

val MainTab.hasCollections: Boolean
    get() = this == MainTab.WALLPAPERS
