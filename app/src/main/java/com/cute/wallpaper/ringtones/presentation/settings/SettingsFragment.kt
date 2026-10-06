package com.cute.wallpaper.ringtones.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.databinding.FragmentSettingsBinding
import com.cute.wallpaper.ringtones.databinding.ItemSettingsRowBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.language.LanguageOptions
import com.leansoft.ads.AdManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {
    @Inject lateinit var preferences: AppPreferences
    private var resumeAdsBeforeExternal: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState?.containsKey(RESUME_ADS) == true) {
            resumeAdsBeforeExternal = savedInstanceState.getBoolean(RESUME_ADS)
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSettingsBinding.inflate(inflater, container, false)

    override fun initView() {
        configure(binding.rowLanguage, R.string.settings_language, R.drawable.ic_settings_globe)
        configure(binding.rowRate, R.string.settings_rate_app, R.drawable.ic_settings_star)
        configure(binding.rowShare, R.string.settings_share_app, R.drawable.ic_settings_share)
        configure(binding.rowPrivacy, R.string.settings_privacy_policy, R.drawable.ic_settings_shield)
        configure(binding.rowDisclaimer, R.string.settings_legal_disclaimer, R.drawable.ic_settings_scale)
        configure(binding.rowDmca, R.string.settings_dmca_policy, R.drawable.ic_settings_file)
        binding.rowLanguage.subtitle.isVisible = true
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener { navViewModel.back() }
        binding.rowLanguage.root.setOnClickListener { navViewModel.navigate(R.id.settingsLanguageFragment) }
        binding.rowRate.root.setOnClickListener {
            suppressResumeAds()
            AdManager.instance.showRatingApp(requireActivity().supportFragmentManager) { restoreResumeAds() }
        }
        binding.rowShare.root.setOnClickListener {
            launchExternal(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${requireContext().packageName}")
            }, getString(R.string.settings_share_app)))
        }
        binding.rowPrivacy.root.setOnClickListener { openLink(R.string.settings_privacy_url) }
        binding.rowDisclaimer.root.setOnClickListener { openLink(R.string.settings_disclaimer_url) }
        binding.rowDmca.root.setOnClickListener { openLink(R.string.settings_dmca_url) }
    }

    override fun onResume() {
        super.onResume()
        binding.rowLanguage.subtitle.text = LanguageOptions.displayName(preferences.languageCode.value)
        restoreResumeAds()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        resumeAdsBeforeExternal?.let { outState.putBoolean(RESUME_ADS, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (isRemoving || activity?.isFinishing == true) restoreResumeAds()
        super.onDestroy()
    }

    private fun configure(row: ItemSettingsRowBinding, title: Int, icon: Int) {
        row.title.setText(title)
        row.icon.setImageResource(icon)
    }

    private fun openLink(resource: Int) {
        val url = getString(resource).trim()
        val uri = Uri.parse(url)
        if (url.isBlank() || uri.scheme !in setOf("https", "http") || uri.host.isNullOrBlank()) {
            toast(R.string.settings_link_unavailable)
            return
        }
        launchExternal(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun launchExternal(intent: Intent) {
        suppressResumeAds()
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            restoreResumeAds()
            toast(R.string.settings_action_unavailable)
        }
    }

    private fun suppressResumeAds() {
        if (resumeAdsBeforeExternal == null) resumeAdsBeforeExternal = AdManager.instance.canShowAdResume
        AdManager.instance.canShowAdResume = false
    }

    private fun restoreResumeAds() {
        resumeAdsBeforeExternal?.let { AdManager.instance.canShowAdResume = it }
        resumeAdsBeforeExternal = null
    }

    private fun toast(message: Int) = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    private companion object { const val RESUME_ADS = "settings_resume_ads_before_external" }
}
