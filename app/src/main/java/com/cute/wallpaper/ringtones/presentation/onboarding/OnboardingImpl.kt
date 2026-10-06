package com.cute.wallpaper.ringtones.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
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
    private var activePlacement: LeansoftAdPlacement? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, placement: LeansoftAdPlacement
    ): View {
        val current: ViewBinding = if (placement in leansofttNativeFullPlacements) {
            FragmentOnboardingNativeFullBinding.inflate(inflater, container, false)
        } else {
            FragmentOnboardingPageBinding.inflate(inflater, container, false)
        }
        bindings[placement] = current
        current.root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                updateHand(placement)
            }
            override fun onViewDetachedFromWindow(v: View) {
                (current as? FragmentOnboardingPageBinding)?.swipeHand?.cancelAnimation()
            }
        })
        return current.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, placement: LeansoftAdPlacement) {
        val current = requireNotNull(bindings[placement])
        view.findViewTreeLifecycleOwner()?.lifecycle?.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                (current as? FragmentOnboardingPageBinding)?.swipeHand?.cancelAnimation()
                if (bindings[placement] === current) {
                    bindings.remove(placement)
                    nextListeners.remove(placement)
                }
            }
        })
        if (current is FragmentOnboardingNativeFullBinding) {
            current.nativeAdContainer.setCollapsibleClick { nextListeners[placement]?.invoke() }
            return
        }
        val binding = current as FragmentOnboardingPageBinding
        val index = when (placement) {
            LeansoftAdPlacement.NATIVE_OBD_1 -> 0
            LeansoftAdPlacement.NATIVE_OBD_2 -> 1
            else -> 2
        }
        binding.tvTitle.setText(titles[index])
        binding.artwork.setImageResource(artworks[index])
        binding.indicator.setImageResource(indicators[index])
        binding.swipeHint.isVisible = index == 1
        binding.btnNext.setText(if (index == 2) R.string.entry_get_started else R.string.continue_label)
        binding.btnNext.setOnClickListener { nextListeners[placement]?.invoke() }
        updateHand(placement)
    }

    override fun onPlacementSelected(placement: LeansoftAdPlacement) {
        activePlacement = placement
        bindings.keys.forEach(::updateHand)
    }

    private fun updateHand(placement: LeansoftAdPlacement) {
        val binding = bindings[placement] as? FragmentOnboardingPageBinding ?: return
        if (activePlacement == placement && binding.swipeHint.isVisible && binding.root.isAttachedToWindow) {
            binding.swipeHand.playAnimation()
        } else binding.swipeHand.cancelAnimation()
    }

    override fun getNativeAdContainer(placement: LeansoftAdPlacement): NativeAdViewContainer =
        when (val binding = requireNotNull(bindings[placement])) {
            is FragmentOnboardingNativeFullBinding -> binding.nativeAdContainer
            is FragmentOnboardingPageBinding -> binding.nativeAdContainer
            else -> error("Unsupported onboarding binding")
        }

    override fun registerNextClick(placement: LeansoftAdPlacement, listener: () -> Unit) { nextListeners[placement] = listener }
    override fun updateUI(placement: LeansoftAdPlacement, needEasy: Boolean) = Unit

    private companion object {
        val titles = intArrayOf(R.string.onboarding_design_title_1, R.string.onboarding_design_title_2, R.string.onboarding_design_title_3)
        val artworks = intArrayOf(R.drawable.img_obd_1, R.drawable.img_obd_2, R.drawable.img_obd_3)
        val indicators = intArrayOf(R.drawable.ic_ob_indicator_1, R.drawable.ic_ob_indicator_2, R.drawable.ic_ob_indicator_3)
    }
}
