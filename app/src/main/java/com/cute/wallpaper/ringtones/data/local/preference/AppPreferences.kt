package com.cute.wallpaper.ringtones.data.local.preference

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private val _languageCode = MutableStateFlow(
        preferences.getString(KEY_LANGUAGE_CODE, null)
    )
    val languageCode: StateFlow<String?> = _languageCode.asStateFlow()

    private val _favoriteKeys = MutableStateFlow(readFavoriteKeys())
    val favoriteKeys: StateFlow<Set<String>> = _favoriteKeys.asStateFlow()

    private val _liveWallpaperUrl = MutableStateFlow(
        preferences.getString(KEY_LIVE_WALLPAPER_URL, null)
    )
    val liveWallpaperUrl: StateFlow<String?> = _liveWallpaperUrl.asStateFlow()

    suspend fun setLanguageCode(code: String) {
        preferences.edit()
            .putString(KEY_LANGUAGE_CODE, code)
            .apply()
        _languageCode.value = code
    }

    suspend fun setLiveWallpaperUrl(url: String) {
        require(url.isNotBlank()) { "Live wallpaper URL must not be blank" }
        preferences.edit()
            .putString(KEY_LIVE_WALLPAPER_URL, url)
            .apply()
        _liveWallpaperUrl.value = url
    }

    suspend fun setFavorite(key: String, isFavorite: Boolean) {
        require(key.isNotBlank()) { "Favorite key must not be blank" }

        val favorites = synchronized(preferences) {
            readFavoriteKeys().toMutableSet().apply {
                if (isFavorite) add(key) else remove(key)
            }.toSet().also { updated ->
                preferences.edit()
                    .putStringSet(KEY_FAVORITE_KEYS, updated)
                    .apply()
            }
        }
        _favoriteKeys.value = favorites
    }

    private fun readFavoriteKeys(): Set<String> {
        return preferences.getStringSet(KEY_FAVORITE_KEYS, emptySet())
            ?.toSet()
            .orEmpty()
    }

    private companion object {
        const val PREFERENCES_NAME = "app_preferences"
        const val KEY_LANGUAGE_CODE = "language_code"
        const val KEY_FAVORITE_KEYS = "favorite_keys"
        const val KEY_LIVE_WALLPAPER_URL = "live_wallpaper_url"
    }
}
