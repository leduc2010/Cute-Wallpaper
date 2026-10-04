package com.cute.wallpaper.ringtones.presentation.home

import androidx.annotation.StringRes
import com.cute.wallpaper.ringtones.R

enum class RingtoneCategory(
    val remoteKey: String,
    @param:StringRes val titleRes: Int
) {
    RINGTONES("ringtones", R.string.ringtone_category_ringtones),
    NOTIFICATIONS("notifications", R.string.ringtone_category_notifications),
    ALARM("alarm", R.string.ringtone_category_alarm),
    TRENDING("trending", R.string.ringtone_category_trending);

    companion object {
        val pages: List<RingtoneCategory> = entries
    }
}
