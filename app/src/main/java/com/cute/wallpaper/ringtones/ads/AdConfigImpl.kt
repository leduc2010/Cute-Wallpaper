package com.cute.wallpaper.ringtones.ads

import com.cute.wallpaper.ringtones.R
import com.leansoft.ads.AdConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdConfigImpl @Inject constructor() : AdConfig() {
    override fun enableAllAds(): Boolean = false
    override fun getLayoutLoading(): Int = R.layout.dialog_loading_ad
}
