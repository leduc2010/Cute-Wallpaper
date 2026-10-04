package com.cute.wallpaper.ringtones.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.utils.AppLocaleManager
import com.cute.wallpaper.ringtones.utils.LanguageCodeNormalizer
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.presentation.main.MainActivity
import com.leansoft.ads.ui.activity.LeansoftSplashActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : LeansoftSplashActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences
    @Inject
    lateinit var appLocaleManager: AppLocaleManager
    @Inject
    lateinit var contentRepository: ContentRepository

    override fun loadedRemoteConfig(isSuccess: Boolean) {
        contentRepository.refresh()
    }

    override fun finishOnboarding(bundle: Bundle) {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun setLanguage(languageCode: String) {
        val normalized = LanguageCodeNormalizer.normalize(languageCode)
        lifecycleScope.launch {
            appPreferences.setLanguageCode(normalized)
            appLocaleManager.applyLanguage(normalized)
        }
    }

    override fun getRemoteConfigDefault(): Int = R.xml.remote_config_defaults
    override fun checkedUpdate(hasUpdate: Boolean) = Unit
}