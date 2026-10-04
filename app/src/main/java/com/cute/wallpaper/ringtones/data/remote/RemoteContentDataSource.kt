package com.cute.wallpaper.ringtones.data.remote

import com.cute.wallpaper.ringtones.data.remote.dto.QuoteDto
import com.cute.wallpaper.ringtones.data.remote.dto.SoundDto
import com.cute.wallpaper.ringtones.data.remote.dto.WallpaperDto
import com.cute.wallpaper.ringtones.data.remote.firebase.FirebaseMgr
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteContentDataSource @Inject constructor(
    private val gson: Gson
) {
    fun getWallpapers(): List<WallpaperDto> =
        parseList(FirebaseMgr.wallpaperData, WallpaperDto::class.java)

    fun getSounds(): List<SoundDto> =
        parseList(FirebaseMgr.soundData, SoundDto::class.java)

    fun getQuotes(): List<QuoteDto> =
        parseList(FirebaseMgr.quoteData, QuoteDto::class.java)

    private fun <T> parseList(json: String, itemClass: Class<T>): List<T> {
        if (json.isBlank()) return emptyList()
        val type = TypeToken.getParameterized(List::class.java, itemClass).type
        return runCatching {
            gson.fromJson<List<T>>(json, type).orEmpty()
        }.getOrDefault(emptyList())
    }
}
