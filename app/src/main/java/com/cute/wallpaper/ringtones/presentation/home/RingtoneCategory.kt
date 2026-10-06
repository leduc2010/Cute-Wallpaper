package com.cute.wallpaper.ringtones.presentation.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.cute.wallpaper.ringtones.R

enum class RingtoneCategory(
    val remoteKey: String,
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int
) {
    RINGTONES(
        "ringtones",
        R.string.ringtone_category_ringtones,
        R.drawable.ic_ringtone_tab_ringtones
    ),
    NOTIFICATIONS(
        "notifications",
        R.string.ringtone_category_notifications,
        R.drawable.ic_ringtone_tab_notifications
    ),
    ALARM(
        "alarm",
        R.string.ringtone_category_alarm,
        R.drawable.ic_ringtone_tab_alarm
    ),
    TRENDING(
        "trending",
        R.string.ringtone_category_trending,
        R.drawable.ic_ringtone_tab_trending
    );

    companion object {
        val pages: List<RingtoneCategory> = entries
    }
}
