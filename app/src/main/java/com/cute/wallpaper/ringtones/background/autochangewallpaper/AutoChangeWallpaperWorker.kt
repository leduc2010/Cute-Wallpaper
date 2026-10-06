package com.cute.wallpaper.ringtones.background.autochangewallpaper

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cute.wallpaper.ringtones.data.local.autochangewallpaper.AutoChangePreferences
import com.cute.wallpaper.ringtones.domain.model.DownloadedWallpaper
import com.cute.wallpaper.ringtones.domain.repository.DownloadedWallpaperStore
import com.cute.wallpaper.ringtones.domain.repository.WallpaperRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutoChangeWallpaperWorkerEntryPoint {
    fun autoChangePreferences(): AutoChangePreferences
    fun downloadedWallpaperStore(): DownloadedWallpaperStore
    fun wallpaperRepository(): WallpaperRepository
}

internal fun orderAutoChangeCandidates(
    wallpapers: List<DownloadedWallpaper>,
    lastAppliedKey: String?
): List<DownloadedWallpaper> {
    if (wallpapers.isEmpty()) return emptyList()

    val lastIndex = wallpapers.indexOfFirst { it.key == lastAppliedKey }
    val startIndex = if (lastIndex < 0 || lastIndex == wallpapers.lastIndex) {
        0
    } else {
        lastIndex + 1
    }

    return wallpapers.indices.map { offset ->
        wallpapers[(startIndex + offset) % wallpapers.size]
    }
}

class AutoChangeWallpaperWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            AutoChangeWallpaperWorkerEntryPoint::class.java
        )
        val preferences = entryPoint.autoChangePreferences()
        val config = preferences.getConfig()
        if (!config.enabled) return Result.success()

        val wallpapers = entryPoint.downloadedWallpaperStore()
            .getValidWallpapers()
        if (wallpapers.isEmpty()) return Result.success()

        val repository = entryPoint.wallpaperRepository()
        val candidates = orderAutoChangeCandidates(
            wallpapers = wallpapers,
            lastAppliedKey = preferences.lastAppliedWallpaperKey()
        )

        candidates.forEach { wallpaper ->
            val applied = repository.setWallpaperFromUri(
                uri = wallpaper.uri,
                target = config.target
            )
            if (applied) {
                preferences.markWallpaperApplied(wallpaper.key)
                return Result.success()
            }
        }

        // Avoid a retry loop when a device rejects every candidate. The next
        // periodic run will try the current valid library again.
        return Result.success()
    }
}
