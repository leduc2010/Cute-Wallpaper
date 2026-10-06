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
import com.bumptech.glide.Glide
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentVideoWallpaperDetailBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.search.SearchMode
import com.cute.wallpaper.ringtones.presentation.search.SearchViewModel
import com.cute.wallpaper.ringtones.service.LiveWallpaperService
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.absoluteValue

@AndroidEntryPoint
class VideoWallpaperDetailFragment : BaseFragment<FragmentVideoWallpaperDetailBinding>() {
    private val viewModel: VideoWallpaperDetailViewModel by viewModels()
    private var currentPosition = 0
    private var isActionMenuExpanded = false
    private lateinit var wallpaperAdapter: WallpaperDetailAdapter
    private var videoPlayer: ExoPlayer? = null
    private var resultMessageHideAction: Runnable? = null

    private val liveWallpaperLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showResultMessage(R.string.wallpaper_success_home)
        }
    }

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            currentPosition = position
            viewModel.selectPage(position)
            wallpaperAdapter.stopPreview()
            setActionMenuExpanded(false, animate = false)
            hideResultMessage()
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
        childFragmentManager.setFragmentResultListener(
            WallpaperTagsDialogFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, result ->
            val tag = result.getString(WallpaperTagsDialogFragment.RESULT_TAG)
                ?.takeIf(String::isNotBlank) ?: return@setFragmentResultListener
            navViewModel.navigate(
                R.id.searchFragment,
                Bundle().apply {
                    putString(SearchViewModel.ARG_MODE, SearchMode.GENRE.name)
                    putString(SearchViewModel.ARG_MAIN_TAB, MainTab.VIDEO_WALLPAPERS.name)
                    putString(SearchViewModel.ARG_QUERY, tag)
                    putString(SearchViewModel.ARG_GENRE_ID, tag)
                },
                NavOptions.Builder()
                    .setPopUpTo(R.id.videoWallpaperDetailFragment, true)
                    .build()
            )
        }

        WindowCompat.getInsetsController(requireActivity().window, binding.root)
            .isAppearanceLightStatusBars = true

        currentPosition = viewModel.currentPage.value
            ?.takeIf { it in viewModel.items.indices }
            ?: viewModel.initialPageIndex
        wallpaperAdapter = WallpaperDetailAdapter(
            items = viewModel.items,
            requestManager = Glide.with(this),
            enableMediaPreview = true,
            videoPlayerProvider = ::getOrCreateVideoPlayer,
            onPreviewError = { showToast(R.string.live_wallpaper_preview_failed) }
        )
        binding.wallpaperPager.apply {
            adapter = wallpaperAdapter
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
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener { navViewModel.back() }
        binding.btnFavorite.setOnClickListener {
            val item = currentItem() ?: return@setOnClickListener
            val favorite = item.ref.toFavoriteKey() in viewModel.favoriteKeys.value.orEmpty()
            viewModel.setFavorite(item.ref, !favorite)
        }
        binding.btnInfo.setOnClickListener { showCurrentTags() }
        binding.primaryActionContainer.setOnClickListener {
            setActionMenuExpanded(!isActionMenuExpanded)
        }
        binding.btnPrimaryAction.setOnClickListener {
            setActionMenuExpanded(!isActionMenuExpanded)
        }
        binding.actionSetLiveWallpaper.setOnClickListener {
            setActionMenuExpanded(false)
            viewModel.prepareLiveWallpaper()
        }
        binding.actionShare.setOnClickListener {
            setActionMenuExpanded(false)
            shareCurrentVideo()
        }
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
    }

    private fun renderInfo() {
        binding.btnInfo.isVisible = currentItem()?.tags?.isNotEmpty() == true
    }

    private fun renderPreparing(preparing: Boolean) {
        binding.wallpaperPager.isUserInputEnabled = !preparing
        binding.btnFavorite.isEnabled = !preparing
        binding.btnInfo.isEnabled = !preparing
        binding.actionShare.isEnabled = !preparing
        binding.actionSetLiveWallpaper.isEnabled = !preparing
        binding.primaryActionContainer.isEnabled = !preparing
        binding.btnPrimaryAction.isVisible = !preparing
        binding.actionProgress.isVisible = preparing
        wallpaperAdapter.setPreviewEnabled(!preparing)
        if (preparing) {
            setActionMenuExpanded(false)
            wallpaperAdapter.stopPreview()
        }
    }

    private fun setActionMenuExpanded(expanded: Boolean, animate: Boolean = true) {
        isActionMenuExpanded = expanded
        binding.btnPrimaryAction.setImageResource(
            if (expanded) R.drawable.ic_close_circle else R.drawable.ic_set_wallpaper
        )
        val menu = binding.actionMenu
        menu.animate().cancel()
        if (!animate) {
            menu.isVisible = expanded
            menu.alpha = 1f
            menu.scaleX = 1f
            menu.scaleY = 1f
            return
        }
        if (expanded) {
            menu.isVisible = true
            menu.alpha = 0f
            menu.scaleX = 0.86f
            menu.scaleY = 0.86f
            menu.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ACTION_ANIMATION_DURATION)
                .start()
        } else if (menu.isVisible) {
            menu.animate()
                .alpha(0f)
                .scaleX(0.86f)
                .scaleY(0.86f)
                .setDuration(ACTION_ANIMATION_DURATION)
                .withEndAction {
                    if (!isActionMenuExpanded) {
                        menu.isVisible = false
                        menu.alpha = 1f
                        menu.scaleX = 1f
                        menu.scaleY = 1f
                    }
                }
                .start()
        }
    }

    private fun shareCurrentVideo() {
        val url = currentItem()?.contentUrl?.takeIf(String::isNotBlank) ?: return
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                },
                getString(R.string.video_wallpaper_share_chooser)
            )
        )
    }

    private fun showResultMessage(messageRes: Int) {
        val messageView = binding.resultMessage
        resultMessageHideAction?.let(messageView::removeCallbacks)
        messageView.animate().cancel()
        messageView.setText(messageRes)
        messageView.alpha = 0f
        messageView.translationY = dp(8).toFloat()
        messageView.isVisible = true
        messageView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(160L)
            .start()
        val hideAction = Runnable {
            messageView.animate()
                .alpha(0f)
                .translationY(dp(8).toFloat())
                .setDuration(140L)
                .withEndAction { messageView.isVisible = false }
                .start()
        }
        resultMessageHideAction = hideAction
        messageView.postDelayed(hideAction, RESULT_MESSAGE_DURATION)
    }

    private fun hideResultMessage() {
        val messageView = binding.resultMessage
        resultMessageHideAction?.let(messageView::removeCallbacks)
        resultMessageHideAction = null
        messageView.animate().cancel()
        messageView.isVisible = false
        messageView.alpha = 1f
        messageView.translationY = 0f
    }

    private fun openLiveWallpaperPreview(url: String) {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(requireContext(), LiveWallpaperService::class.java)
            )
        }
        runCatching { liveWallpaperLauncher.launch(intent) }
            .recoverCatching {
                liveWallpaperLauncher.launch(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            }
            .onFailure {
                showToast(R.string.live_wallpaper_preview_failed)
            }
    }

    private fun showCurrentTags() {
        val tags = currentItem()?.tags.orEmpty()
        if (tags.isEmpty()) return
        if (childFragmentManager.findFragmentByTag(WallpaperTagsDialogFragment.TAG) != null) {
            return
        }
        WallpaperTagsDialogFragment.newInstance(tags).show(
            childFragmentManager,
            WallpaperTagsDialogFragment.TAG
        )
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun getOrCreateVideoPlayer(): ExoPlayer? {
        if (!isAdded || !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
            return null
        }
        return videoPlayer ?: ExoPlayer.Builder(requireContext()).build().also {
            videoPlayer = it
        }
    }

    override fun onPause() {
        wallpaperAdapter.stopPreview()
        super.onPause()
    }

    override fun onStop() {
        wallpaperAdapter.stopPreview()
        videoPlayer?.release()
        videoPlayer = null
        super.onStop()
    }

    override fun onDestroyView() {
        resultMessageHideAction?.let(binding.resultMessage::removeCallbacks)
        resultMessageHideAction = null
        binding.resultMessage.animate().cancel()
        binding.actionMenu.animate().cancel()
        wallpaperAdapter.stopPreview()
        binding.wallpaperPager.unregisterOnPageChangeCallback(pageCallback)
        binding.wallpaperPager.adapter = null
        super.onDestroyView()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val ACTION_ANIMATION_DURATION = 180L
        const val RESULT_MESSAGE_DURATION = 1800L
    }
}
