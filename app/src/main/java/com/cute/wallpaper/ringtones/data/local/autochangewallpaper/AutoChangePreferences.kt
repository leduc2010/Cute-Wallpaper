package com.cute.wallpaper.ringtones.data.local.autochangewallpaper

import android.content.Context
import com.cute.wallpaper.ringtones.domain.model.AutoChangeConfig
import com.cute.wallpaper.ringtones.domain.model.WallpaperTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoChangePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getConfig(): AutoChangeConfig {
        val target = runCatching {
            WallpaperTarget.valueOf(
                preferences.getString(KEY_TARGET, null)
                    ?: WallpaperTarget.BOTH.name
            )
        }.getOrDefault(WallpaperTarget.BOTH)

        val intervalHours = preferences.getInt(
            KEY_INTERVAL_HOURS,
            AutoChangeConfig.DEFAULT_INTERVAL_HOURS
        ).takeIf { it in AutoChangeConfig.SUPPORTED_INTERVAL_HOURS }
            ?: AutoChangeConfig.DEFAULT_INTERVAL_HOURS

        return AutoChangeConfig(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            target = target,
            intervalHours = intervalHours
        )
    }

    fun saveConfig(
        enabled: Boolean,
        target: WallpaperTarget,
        intervalHours: Int
    ) {
        require(intervalHours in AutoChangeConfig.SUPPORTED_INTERVAL_HOURS) {
            "Unsupported auto-change interval: $intervalHours"
        }
        preferences.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_TARGET, target.name)
            .putInt(KEY_INTERVAL_HOURS, intervalHours)
            .apply()
    }

    fun setEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun lastAppliedWallpaperKey(): String? {
        return preferences.getString(KEY_LAST_WALLPAPER, null)
    }

    fun markWallpaperApplied(key: String) {
        preferences.edit()
            .putString(KEY_LAST_WALLPAPER, key)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "auto_change_preferences"
        const val KEY_ENABLED = "enabled"
        const val KEY_TARGET = "target"
        const val KEY_INTERVAL_HOURS = "interval_hours"
        const val KEY_LAST_WALLPAPER = "last_wallpaper"
    }
}
