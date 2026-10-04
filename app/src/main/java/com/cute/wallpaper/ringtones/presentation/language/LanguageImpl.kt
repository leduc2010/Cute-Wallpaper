package com.cute.wallpaper.ringtones.presentation.language

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cute.wallpaper.ringtones.utils.LanguageCodeNormalizer
import com.cute.wallpaper.ringtones.databinding.FragmentLanguageBinding
import com.leansoft.ads.ui.language.LeansoftLanguageInterface
import com.leansoft.ads.view.NativeAdViewContainer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageImpl @Inject constructor() : LeansoftLanguageInterface {
    private var binding: FragmentLanguageBinding? = null
    private var selectLanguageEvent: ((String) -> Unit)? = null
    private var setLanguageEvent: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentLanguageBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        languageCodeSelected: String?
    ) {
        val currentBinding = requireNotNull(binding)
        val initialCode = languageCodeSelected?.takeIf { it.isNotBlank() }
        val adapter = LanguageAdapter(
            items = languageList(),
            selectedLanguageCode = initialCode
        ) { item ->
            currentBinding.btnNext.isEnabled = true
            selectLanguageEvent?.invoke(item.code)
        }
        currentBinding.recyclerView.adapter = adapter
        currentBinding.btnNext.isEnabled = initialCode != null
        currentBinding.btnNext.setOnClickListener {
            adapter.selectedLanguageCode?.let { setLanguageEvent?.invoke(it) }
        }
    }

    override fun getNativeAdContainer(): NativeAdViewContainer {
        return requireNotNull(binding).nativeAdContainer
    }

    override fun registerSelectLanguageEvent(listener: (String) -> Unit) {
        selectLanguageEvent = listener
    }

    override fun registerSetLanguageEvent(listener: (String) -> Unit) {
        setLanguageEvent = listener
    }

    override fun updateUI(needEasy: Boolean) = Unit

    private fun languageList(): List<LanguageItem> {
        val source = listOf(
            LanguageItem("en", "English"),
            LanguageItem("es", "Español"),
            LanguageItem("fr", "Français"),
            LanguageItem("de", "Deutsch"),
            LanguageItem("pt-BR", "Português (Brasil)"),
            LanguageItem("vi", "Tiếng Việt"),
            LanguageItem("id", "Bahasa Indonesia"),
            LanguageItem("en-PH", "English (Philippines)")
        )
        val device = LanguageCodeNormalizer.normalize(Locale.getDefault().toLanguageTag())
        val deviceLanguage = device.substringBefore('-')
        val deviceItem = source.firstOrNull { item ->
            val code = LanguageCodeNormalizer.normalize(item.code)
            code == device || code.substringBefore('-') == deviceLanguage
        }
        if (deviceItem == null) return source
        return buildList {
            add(deviceItem)
            source.firstOrNull { it.code == "en" && it != deviceItem }?.let(::add)
            addAll(source.filterNot { it == deviceItem || it.code == "en" })
        }
    }
}
