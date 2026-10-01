package com.cute.wallpaper.ringtones.presentation.welcome_back

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cute.wallpaper.ringtones.databinding.FragmentWelcomeBackBinding
import com.leansoft.ads.ui.welcome_back.LeansoftWelcomeBackInterface
import com.leansoft.ads.view.NativeAdViewContainer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WelcomeBackImpl @Inject constructor() : LeansoftWelcomeBackInterface() {
    private var binding: FragmentWelcomeBackBinding? = null
    private var listener: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentWelcomeBackBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireNotNull(binding).btnContinue.setOnClickListener { listener?.invoke() }
    }

    override fun getNativeAdViewContainer(): NativeAdViewContainer {
        return requireNotNull(binding).nativeAdContainer
    }

    override fun updateCountdownSecond(remainTime: Int) = Unit

    override fun updateEnableButtonNext(enable: Boolean) {
        binding?.btnContinue?.isEnabled = enable
    }

    override fun registerGoToMainListener(listener: () -> Unit) {
        this.listener = listener
    }
}
