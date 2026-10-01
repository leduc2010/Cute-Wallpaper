package com.cute.wallpaper.ringtones.data.local.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

    suspend fun setLanguageCode(code: String) {
        context.appDataStore.edit { preferences ->
            preferences[LANGUAGE_CODE] = code
        }
    }

    private companion object {
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
    }
}
