package com.cute.wallpaper.ringtones.presentation.uninstall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cute.wallpaper.ringtones.databinding.FragmentUninstallBinding
import com.leansoft.ads.ui.uninstall.LeansoftUninstallInterface
import com.leansoft.ads.view.NativeAdViewContainer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UninstallImpl @Inject constructor() : LeansoftUninstallInterface {
    private var binding: FragmentUninstallBinding? = null
    private var listener: ((Bundle) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentUninstallBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireNotNull(binding).btnTryAgain.setOnClickListener {
            listener?.invoke(Bundle())
        }
    }

    override fun getNativeAdViewContainer(): NativeAdViewContainer {
        return requireNotNull(binding).nativeAdContainer
    }

    override fun registerGoToMainListener(listener: (Bundle) -> Unit) {
        this.listener = listener
    }
}
