package com.cute.wallpaper.ringtones.presentation.language

import androidx.annotation.DrawableRes

data class LanguageItem(
    val code: String,
    val displayName: String,
    @param:DrawableRes val flagRes: Int
)
