package com.cute.wallpaper.ringtones.presentation.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentWallpaperSuccessBinding
import com.cute.wallpaper.ringtones.domain.model.WallpaperTarget
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment

class WallpaperSuccessFragment : BaseFragment<FragmentWallpaperSuccessBinding>() {
    private val previewUrl: String by lazy {
        arguments?.getString(ARG_PREVIEW_URL).orEmpty()
    }
    private val target: WallpaperTarget by lazy {
        runCatching {
            WallpaperTarget.valueOf(arguments?.getString(ARG_TARGET).orEmpty())
        }.getOrDefault(WallpaperTarget.BOTH)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentWallpaperSuccessBinding {
        return FragmentWallpaperSuccessBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        WindowCompat.getInsetsController(requireActivity().window, binding.root)
            .isAppearanceLightStatusBars = true

        Glide.with(binding.wallpaperPreview)
            .load(previewUrl)
            .centerCrop()
            .into(binding.wallpaperPreview)
        binding.successDescription.setText(
            when (target) {
                WallpaperTarget.HOME -> R.string.wallpaper_success_home
                WallpaperTarget.LOCK -> R.string.wallpaper_success_lock
                WallpaperTarget.BOTH -> R.string.wallpaper_success_both
            }
        )
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener { navViewModel.back() }
        binding.btnHome.setOnClickListener { navViewModel.backTo(R.id.mainFragment) }
    }

    companion object {
        const val ARG_PREVIEW_URL = "wallpaper_success_preview_url"
        const val ARG_TARGET = "wallpaper_success_target"
    }
}
