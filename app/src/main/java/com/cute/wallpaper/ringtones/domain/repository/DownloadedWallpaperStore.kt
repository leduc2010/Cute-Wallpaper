package com.cute.wallpaper.ringtones.domain.repository

import com.cute.wallpaper.ringtones.domain.model.DownloadedWallpaper
import kotlinx.coroutines.flow.StateFlow

interface DownloadedWallpaperStore {
    val wallpapers: StateFlow<List<DownloadedWallpaper>>

    suspend fun add(key: String, uri: String)

    suspend fun refresh()

    suspend fun getValidWallpapers(): List<DownloadedWallpaper>
}
