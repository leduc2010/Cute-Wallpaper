package com.cute.wallpaper.ringtones.presentation.splash

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import com.cute.wallpaper.ringtones.databinding.FragmentSplashBinding
import com.leansoft.ads.ui.splash.LeansoftSplashInterface
import com.leansoft.ads.view.BannerAdViewContainer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SplashImpl @Inject constructor() : LeansoftSplashInterface() {
    private var binding: FragmentSplashBinding? = null
    private var progressAnimator: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentSplashBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val currentBinding = requireNotNull(binding)
        currentBinding.topProgressView.progress = 0f
        progressAnimator?.cancel()
        progressAnimator = ObjectAnimator.ofFloat(
            currentBinding.topProgressView,
            "progress",
            0f,
            1f
        ).apply {
            duration = TOP_PROGRESS_DURATION_MS
            interpolator = LinearInterpolator()
            start()
        }
    }

    override fun getBannerAdContainer(): BannerAdViewContainer {
        return requireNotNull(binding).bannerAdContainer
    }

    private companion object {
        const val TOP_PROGRESS_DURATION_MS = 8000L
    }
}
