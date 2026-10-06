package com.cute.wallpaper.ringtones.presentation.language

import android.content.res.Resources
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.cute.wallpaper.ringtones.databinding.FragmentLanguageBinding
import com.leansoft.ads.ui.language.LeansoftLanguageInterface
import com.leansoft.ads.view.NativeAdViewContainer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageImpl @Inject constructor() : LeansoftLanguageInterface {
    private var binding: FragmentLanguageBinding? = null
    private var selectLanguageEvent: ((String) -> Unit)? = null
    private var setLanguageEvent: ((String) -> Unit)? = null
    private var scrollState: Parcelable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val current = FragmentLanguageBinding.inflate(inflater, container, false)
        binding = current
        current.root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                scrollState = current.recyclerView.layoutManager?.onSaveInstanceState()
            }
        })
        return current.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, languageCodeSelected: String?) {
        val current = requireNotNull(binding)
        view.findViewTreeLifecycleOwner()?.lifecycle?.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                scrollState = current.recyclerView.layoutManager?.onSaveInstanceState()
                current.recyclerView.adapter = null
                if (binding === current) {
                    binding = null
                    selectLanguageEvent = null
                    setLanguageEvent = null
                }
            }
        })
        val selected = languageCodeSelected?.takeIf(String::isNotBlank)
        val adapter = LanguageAdapter(
            items = LanguageOptions.forDevice(Resources.getSystem().configuration.locales[0].toLanguageTag()),
            selectedLanguageCode = selected,
            showHandClick = selected == null
        ) { item ->
            current.btnNext.isEnabled = true
            current.btnNext.isVisible = true
            // The SDK can recreate LFO after this callback; capture position first.
            scrollState = current.recyclerView.layoutManager?.onSaveInstanceState()
            if (selected == null) selectLanguageEvent?.invoke(item.code)
        }
        current.recyclerView.adapter = adapter
        current.recyclerView.itemAnimator = null
        scrollState?.let { current.recyclerView.layoutManager?.onRestoreInstanceState(it) }
        current.btnNext.isEnabled = selected != null
        current.btnNext.isVisible = selected != null
        current.btnNext.setOnClickListener {
            adapter.selectedLanguageCode?.let { setLanguageEvent?.invoke(it) }
        }
    }

    override fun getNativeAdContainer(): NativeAdViewContainer = requireNotNull(binding).nativeAdContainer
    override fun registerSelectLanguageEvent(listener: (String) -> Unit) { selectLanguageEvent = listener }
    override fun registerSetLanguageEvent(listener: (String) -> Unit) { setLanguageEvent = listener }
    override fun updateUI(needEasy: Boolean) = Unit
}
