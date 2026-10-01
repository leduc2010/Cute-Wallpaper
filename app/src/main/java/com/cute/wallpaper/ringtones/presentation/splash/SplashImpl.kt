package com.cute.wallpaper.ringtones.presentation.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cute.wallpaper.ringtones.databinding.FragmentSplashBinding
import com.leansoft.ads.ui.splash.LeansoftSplashInterface
import com.leansoft.ads.view.BannerAdViewContainer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SplashImpl @Inject constructor() : LeansoftSplashInterface() {
    private var binding: FragmentSplashBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentSplashBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = Unit

    override fun getBannerAdContainer(): BannerAdViewContainer {
        return requireNotNull(binding).bannerAdContainer
    }
}
