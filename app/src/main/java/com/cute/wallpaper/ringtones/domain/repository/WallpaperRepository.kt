package com.cute.wallpaper.ringtones.domain.repository

import com.cute.wallpaper.ringtones.domain.model.WallpaperTarget

interface WallpaperRepository {
    suspend fun setWallpaper(
        lockUrl: String,
        homeUrl: String?,
        target: WallpaperTarget
    ): Boolean

    suspend fun downloadWallpaper(url: String, fileName: String): String?

    suspend fun setWallpaperFromUri(uri: String, target: WallpaperTarget): Boolean

    suspend fun prepareShareWallpaper(url: String, fileName: String): String?
}
