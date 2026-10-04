package com.cute.wallpaper.ringtones.data.remote.firebase

import com.google.firebase.remoteconfig.FirebaseRemoteConfig

object FirebaseMgr {
    private val config: FirebaseRemoteConfig
        get() = FirebaseRemoteConfig.getInstance()

    val wallpaperData: String
        get() = config.getString(KEY_WALLPAPER_DATA)

    val soundData: String
        get() = config.getString(KEY_SOUND_DATA)

    val quoteData: String
        get() = config.getString(KEY_QUOTE_DATA)

    private const val KEY_WALLPAPER_DATA = "wallpaper_data"
    private const val KEY_SOUND_DATA = "sound_data"
    private const val KEY_QUOTE_DATA = "quote_data"
}
