package com.cute.wallpaper.ringtones.presentation.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentWallpaperSuccessBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment

class LiveWallpaperSuccessFragment : BaseFragment<FragmentWallpaperSuccessBinding>() {
    private val previewUrl: String by lazy {
        arguments?.getString(ARG_PREVIEW_URL).orEmpty()
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val safeArea = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(top = safeArea.top, left = safeArea.left, right = safeArea.right)
            insets
        }
        Glide.with(binding.wallpaperPreview)
            .load(previewUrl)
            .centerCrop()
            .into(binding.wallpaperPreview)
        binding.successDescription.setText(R.string.live_wallpaper_success)
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener { navViewModel.back() }
        binding.btnHome.setOnClickListener { navViewModel.backTo(R.id.mainFragment) }
    }

    companion object {
        const val ARG_PREVIEW_URL = "live_wallpaper_success_preview_url"
    }
}
