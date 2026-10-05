package com.cute.wallpaper.ringtones.presentation.detail

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentVideoWallpaperDetailBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.service.LiveWallpaperService
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.absoluteValue

@AndroidEntryPoint
class VideoWallpaperDetailFragment : BaseFragment<FragmentVideoWallpaperDetailBinding>() {
    private val viewModel: VideoWallpaperDetailViewModel by viewModels()
    private var currentPosition = 0
    private var pendingPreviewUrl: String? = null

    private val liveWallpaperLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val previewUrl = pendingPreviewUrl ?: currentItem()?.contentUrl ?: return@registerForActivityResult
            navViewModel.navigate(
                R.id.liveWallpaperSuccessFragment,
                Bundle().apply {
                    putString(LiveWallpaperSuccessFragment.ARG_PREVIEW_URL, previewUrl)
                }
            )
        }
        pendingPreviewUrl = null
    }

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            currentPosition = position
            viewModel.selectPage(position)
            renderFavorite()
            renderInfo()
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentVideoWallpaperDetailBinding {
        return FragmentVideoWallpaperDetailBinding.inflate(inflater, container, false)
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

        currentPosition = viewModel.currentPage.value
            ?.takeIf { it in viewModel.items.indices }
            ?: viewModel.initialPageIndex
        binding.wallpaperPager.apply {
            adapter = WallpaperDetailAdapter(viewModel.items)
            isSaveEnabled = false
            offscreenPageLimit = 2
            setCurrentItem(currentPosition, false)
            registerOnPageChangeCallback(pageCallback)
            setPageTransformer(
                CompositePageTransformer().apply {
                    addTransformer(MarginPageTransformer(dp(10)))
                    addTransformer { page, position ->
                        page.scaleY = 1f - (0.04f * position.absoluteValue.coerceAtMost(1f))
                    }
                }
            )
            (getChildAt(0) as? RecyclerView)?.apply {
                clipChildren = false
                clipToPadding = false
                overScrollMode = View.OVER_SCROLL_NEVER
            }
        }
        renderFavorite()
        renderInfo()
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener { navViewModel.back() }
        binding.btnFavorite.setOnClickListener {
            val item = currentItem() ?: return@setOnClickListener
            val favorite = item.ref.toFavoriteKey() in viewModel.favoriteKeys.value.orEmpty()
            viewModel.setFavorite(item.ref, !favorite)
        }
        binding.btnInfo.setOnClickListener { showCurrentTags() }
        binding.btnSetLiveWallpaper.setOnClickListener { viewModel.prepareLiveWallpaper() }
    }

    override fun observeData() {
        viewModel.favoriteKeys.observe(viewLifecycleOwner) { renderFavorite() }
        viewModel.isPreparing.observe(viewLifecycleOwner, ::renderPreparing)
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (val content = event.getContentIfNotHandled()) {
                is LiveWallpaperEvent.LaunchPreview -> openLiveWallpaperPreview(content.url)
                LiveWallpaperEvent.PrepareFailed -> showToast(R.string.live_wallpaper_preview_failed)
                null -> Unit
            }
        }
    }

    private fun currentItem(): HomeContentUiModel? = viewModel.items.getOrNull(currentPosition)

    private fun renderFavorite() {
        val item = currentItem()
        binding.btnFavorite.isEnabled = item != null
        val favorite = item?.ref?.toFavoriteKey() in viewModel.favoriteKeys.value.orEmpty()
        binding.btnFavorite.setImageResource(
            if (favorite) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
        item?.let {
            binding.btnFavorite.contentDescription = getString(
                if (favorite) R.string.home_remove_favorite else R.string.home_add_favorite,
                it.title
            )
        }
    }

    private fun renderInfo() {
        binding.btnInfo.isVisible = currentItem()?.tags?.isNotEmpty() == true
    }

    private fun renderPreparing(preparing: Boolean) {
        binding.btnSetLiveWallpaper.isEnabled = !preparing
        binding.wallpaperPager.isUserInputEnabled = !preparing
        binding.actionProgress.isVisible = preparing
        binding.btnSetLiveWallpaper.text = getString(
            if (preparing) R.string.live_wallpaper_preparing else R.string.live_wallpaper_set
        )
    }

    private fun openLiveWallpaperPreview(url: String) {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(requireContext(), LiveWallpaperService::class.java)
            )
        }
        if (intent.resolveActivity(requireContext().packageManager) == null) {
            showToast(R.string.live_wallpaper_not_supported)
            return
        }
        pendingPreviewUrl = url
        runCatching { liveWallpaperLauncher.launch(intent) }
            .onFailure {
                pendingPreviewUrl = null
                showToast(R.string.live_wallpaper_preview_failed)
            }
    }

    private fun showCurrentTags() {
        val tags = currentItem()?.tags.orEmpty()
        if (tags.isEmpty()) return
        Toast.makeText(requireContext(), tags.joinToString(" · "), Toast.LENGTH_SHORT).show()
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        binding.wallpaperPager.unregisterOnPageChangeCallback(pageCallback)
        binding.wallpaperPager.adapter = null
        super.onDestroyView()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
