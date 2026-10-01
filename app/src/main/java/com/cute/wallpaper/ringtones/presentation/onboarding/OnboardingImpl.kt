package com.cute.wallpaper.ringtones.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentOnboardingNativeFullBinding
import com.cute.wallpaper.ringtones.databinding.FragmentOnboardingPageBinding
import com.leansoft.ads.ui.onboarding.LeansoftOnboardingInterface
import com.leansoft.ads.utils.LeansoftAdPlacement
import com.leansoft.ads.utils.leansofttNativeFullPlacements
import com.leansoft.ads.view.NativeAdViewContainer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingImpl @Inject constructor() : LeansoftOnboardingInterface() {
    private val bindings = mutableMapOf<LeansoftAdPlacement, ViewBinding>()
    private val nextListeners = mutableMapOf<LeansoftAdPlacement, () -> Unit>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
        placement: LeansoftAdPlacement
    ): View {
        return if (leansofttNativeFullPlacements.contains(placement)) {
            FragmentOnboardingNativeFullBinding.inflate(inflater, container, false)
                .also { bindings[placement] = it }
                .root
        } else {
            FragmentOnboardingPageBinding.inflate(inflater, container, false)
                .also { bindings[placement] = it }
                .root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, placement: LeansoftAdPlacement) {
        if (leansofttNativeFullPlacements.contains(placement)) {
            val binding = bindings[placement] as FragmentOnboardingNativeFullBinding
            binding.nativeAdContainer.setCollapsibleClick {
                nextListeners[placement]?.invoke()
            }
            return
        }

        val binding = bindings[placement] as FragmentOnboardingPageBinding
        val content = when (placement) {
            LeansoftAdPlacement.NATIVE_OBD_1 -> R.string.onboarding_title_1 to R.string.onboarding_description_1
            LeansoftAdPlacement.NATIVE_OBD_2 -> R.string.onboarding_title_2 to R.string.onboarding_description_2
            else -> R.string.onboarding_title_3 to R.string.onboarding_description_3
        }
        binding.tvTitle.setText(content.first)
        binding.tvDescription.setText(content.second)
        binding.btnNext.setOnClickListener { nextListeners[placement]?.invoke() }
    }

    override fun getNativeAdContainer(placement: LeansoftAdPlacement): NativeAdViewContainer {
        return if (leansofttNativeFullPlacements.contains(placement)) {
            (bindings[placement] as FragmentOnboardingNativeFullBinding).nativeAdContainer
        } else {
            (bindings[placement] as FragmentOnboardingPageBinding).nativeAdContainer
        }
    }

    override fun registerNextClick(placement: LeansoftAdPlacement, listener: () -> Unit) {
        nextListeners[placement] = listener
    }

    override fun updateUI(placement: LeansoftAdPlacement, needEasy: Boolean) = Unit
}
