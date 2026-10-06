package com.cute.wallpaper.ringtones.data.local.autochangewallpaper

import android.content.Context
import android.net.Uri
import com.cute.wallpaper.ringtones.domain.model.DownloadedWallpaper
import com.cute.wallpaper.ringtones.domain.repository.DownloadedWallpaperStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadedWallpaperStoreImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DownloadedWallpaperStore {

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val mutex = Mutex()

    private val _wallpapers = MutableStateFlow<List<DownloadedWallpaper>>(emptyList())
    override val wallpapers: StateFlow<List<DownloadedWallpaper>> =
        _wallpapers.asStateFlow()

    override suspend fun add(key: String, uri: String) {
        require(key.isNotBlank()) { "Downloaded wallpaper key must not be blank" }
        require(uri.isNotBlank()) { "Downloaded wallpaper URI must not be blank" }

        withContext(Dispatchers.IO) {
            mutex.withLock {
                val updated = readStoredWallpapers()
                    .filterNot { it.key == key }
                    .plus(DownloadedWallpaper(key = key, uri = uri))
                persist(updated)
                _wallpapers.value = updated
            }
        }
    }

    override suspend fun refresh() {
        getValidWallpapers()
    }

    override suspend fun getValidWallpapers(): List<DownloadedWallpaper> {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val stored = readStoredWallpapers()
                val valid = stored.filter(::isReadable)
                if (valid != stored) {
                    persist(valid)
                }
                _wallpapers.value = valid
                valid
            }
        }
    }

    private fun isReadable(wallpaper: DownloadedWallpaper): Boolean {
        return runCatching {
            val uri = Uri.parse(wallpaper.uri)
            when (uri.scheme) {
                "content" -> context.contentResolver.openInputStream(uri)
                    ?.use { input -> input.read() != -1 }
                    ?: false

                "file" -> {
                    val path = uri.path ?: return@runCatching false
                    File(path).let { it.isFile && it.length() > 0L }
                }

                else -> false
            }
        }.getOrDefault(false)
    }

    private fun readStoredWallpapers(): List<DownloadedWallpaper> {
        val raw = preferences.getString(KEY_WALLPAPERS, null).orEmpty()
        if (raw.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val key = item.optString(FIELD_KEY)
                    val uri = item.optString(FIELD_URI)
                    if (key.isNotBlank() && uri.isNotBlank()) {
                        add(DownloadedWallpaper(key = key, uri = uri))
                    }
                }
            }.distinctBy(DownloadedWallpaper::key)
        }.getOrDefault(emptyList())
    }

    private fun persist(wallpapers: List<DownloadedWallpaper>) {
        val array = JSONArray()
        wallpapers.forEach { wallpaper ->
            array.put(
                JSONObject()
                    .put(FIELD_KEY, wallpaper.key)
                    .put(FIELD_URI, wallpaper.uri)
            )
        }
        preferences.edit()
            .putString(KEY_WALLPAPERS, array.toString())
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "downloaded_wallpaper_library"
        const val KEY_WALLPAPERS = "wallpapers"
        const val FIELD_KEY = "key"
        const val FIELD_URI = "uri"
    }
}
