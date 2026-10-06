package com.cute.wallpaper.ringtones.presentation.autochangewallpaper

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.background.autochangewallpaper.AutoChangeWallpaperScheduler
import com.cute.wallpaper.ringtones.data.local.autochangewallpaper.AutoChangePreferences
import com.cute.wallpaper.ringtones.domain.model.AutoChangeConfig
import com.cute.wallpaper.ringtones.domain.model.DownloadedWallpaper
import com.cute.wallpaper.ringtones.domain.repository.DownloadedWallpaperStore
import com.cute.wallpaper.ringtones.domain.model.WallpaperTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutoChangeWallpaperViewModel @Inject constructor(
    private val preferences: AutoChangePreferences,
    private val downloadedWallpaperStore: DownloadedWallpaperStore,
    private val scheduler: AutoChangeWallpaperScheduler
) : ViewModel() {

    val downloadedWallpapers: LiveData<List<DownloadedWallpaper>> =
        downloadedWallpaperStore.wallpapers.asLiveData()

    init {
        viewModelScope.launch {
            downloadedWallpaperStore.refresh()
        }
    }

    fun currentConfig(): AutoChangeConfig = preferences.getConfig()

    fun enableOrSave(target: WallpaperTarget, intervalHours: Int) {
        preferences.saveConfig(
            enabled = true,
            target = target,
            intervalHours = intervalHours
        )
        scheduler.schedule(intervalHours)
    }

    fun stop() {
        preferences.setEnabled(false)
        scheduler.cancel()
    }
}
