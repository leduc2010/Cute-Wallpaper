package com.cute.wallpaper.ringtones.background.autochangewallpaper

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cute.wallpaper.ringtones.domain.model.AutoChangeConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoChangeWallpaperScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val UNIQUE_WORK_NAME = "auto_change_wallpaper"
    }

    fun schedule(intervalHours: Int) {
        require(intervalHours in AutoChangeConfig.SUPPORTED_INTERVAL_HOURS) {
            "Unsupported auto-change interval: $intervalHours"
        }

        val request = PeriodicWorkRequestBuilder<AutoChangeWallpaperWorker>(
            intervalHours.toLong(),
            TimeUnit.HOURS
        )
            .setInitialDelay(intervalHours.toLong(), TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
