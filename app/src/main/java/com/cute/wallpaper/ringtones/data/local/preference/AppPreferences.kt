package com.cute.wallpaper.ringtones.data.local.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appDataStore by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val languageCode: Flow<String?> = context.appDataStore.data.map { preferences ->
        preferences[LANGUAGE_CODE]
    }

    val favoriteKeys: Flow<Set<String>> = context.appDataStore.data.map { preferences ->
        preferences[FAVORITE_KEYS].orEmpty()
    }

    suspend fun setLanguageCode(code: String) {
        context.appDataStore.edit { preferences ->
            preferences[LANGUAGE_CODE] = code
        }
    }

    suspend fun setFavorite(key: String, isFavorite: Boolean) {
        require(key.isNotBlank()) { "Favorite key must not be blank" }

        context.appDataStore.edit { preferences ->
            val favorites = preferences[FAVORITE_KEYS].orEmpty().toMutableSet()
            if (isFavorite) {
                favorites += key
            } else {
                favorites -= key
            }
            preferences[FAVORITE_KEYS] = favorites
        }
    }

    private companion object {
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val FAVORITE_KEYS = stringSetPreferencesKey("favorite_keys")
    }
}
