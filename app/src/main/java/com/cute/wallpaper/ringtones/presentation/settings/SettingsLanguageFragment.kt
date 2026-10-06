package com.cute.wallpaper.ringtones.presentation.settings

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.databinding.FragmentLanguageBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.language.LanguageAdapter
import com.cute.wallpaper.ringtones.presentation.language.LanguageOptions
import com.cute.wallpaper.ringtones.utils.AppLocaleManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsLanguageFragment : BaseFragment<FragmentLanguageBinding>() {
    @Inject lateinit var preferences: AppPreferences
    @Inject lateinit var localeManager: AppLocaleManager
    private var selectedCode: String? = null
    private var adapter: LanguageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedCode = savedInstanceState?.getString(SELECTED_CODE) ?: preferences.languageCode.value ?: "en"
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentLanguageBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.nativeAdContainer.isVisible = false
        binding.btnBack.isVisible = true
        binding.tvTitle.updatePadding(left = dp(48))
        binding.btnNext.isVisible = true
        binding.btnNext.isEnabled = true
        adapter = LanguageAdapter(
            LanguageOptions.forDevice(Resources.getSystem().configuration.locales[0].toLanguageTag()),
            selectedCode
        ) { selectedCode = it.code }.also { selectedCode = it.selectedLanguageCode }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.itemAnimator = null
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener { navViewModel.back() }
        binding.btnNext.setOnClickListener {
            val code = selectedCode ?: return@setOnClickListener
            binding.btnNext.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                preferences.setLanguageCode(code)
                navViewModel.back()
                localeManager.applyLanguage(code)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SELECTED_CODE, selectedCode)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        adapter = null
        super.onDestroyView()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private companion object { const val SELECTED_CODE = "settings_selected_language" }
}
