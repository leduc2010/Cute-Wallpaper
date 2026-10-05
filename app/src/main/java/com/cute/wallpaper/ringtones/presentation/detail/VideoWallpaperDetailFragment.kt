package com.cute.wallpaper.ringtones.presentation.detail

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
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
    private var isActionMenuExpanded = false
    private var loadedVideoUrl: String? = null
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
            stopVideoPreview()
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
        binding.videoPreview.setOnCompletionListener {
            binding.btnPlay.setImageResource(R.drawable.ic_play)
            binding.btnPlay.isVisible = true
        }
        binding.videoPreview.setOnErrorListener { _, _, _ ->
            stopVideoPreview()
            showToast(R.string.live_wallpaper_preview_failed)
            true
        }
        renderFavorite()
        renderInfo()
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener { navViewModel.back() }
        binding.btnPlay.setOnClickListener { toggleVideoPreview() }
        binding.videoPreview.setOnClickListener { toggleVideoPreview() }
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
        binding.wallpaperPager.isUserInputEnabled = !preparing
        binding.btnFavorite.isEnabled = !preparing
        binding.btnInfo.isEnabled = !preparing
        binding.btnPlay.isEnabled = !preparing
        binding.actionShare.isEnabled = !preparing
        binding.actionSetLiveWallpaper.isEnabled = !preparing
        binding.primaryActionContainer.isEnabled = !preparing
        binding.btnPrimaryAction.isVisible = !preparing
        binding.actionProgress.isVisible = preparing
        if (preparing) {
            setActionMenuExpanded(false)
            stopVideoPreview()
        }
    }

    private fun toggleVideoPreview() {
        val url = currentItem()?.contentUrl?.takeIf(String::isNotBlank) ?: return
        if (binding.videoPreview.isPlaying) {
            binding.videoPreview.pause()
            binding.btnPlay.setImageResource(R.drawable.ic_play)
            binding.btnPlay.isVisible = true
            return
        }
        binding.videoPreview.isVisible = true
        if (loadedVideoUrl != url) {
            loadedVideoUrl = url
            binding.videoPreview.setVideoURI(Uri.parse(url))
            binding.videoPreview.setOnPreparedListener { player ->
                player.isLooping = true
                binding.videoPreview.start()
                binding.btnPlay.isVisible = false
            }
        } else {
            binding.videoPreview.start()
            binding.btnPlay.isVisible = false
        }
    }

    private fun stopVideoPreview() {
        runCatching { binding.videoPreview.stopPlayback() }
        loadedVideoUrl = null
        binding.videoPreview.isVisible = false
        binding.btnPlay.setImageResource(R.drawable.ic_play)
        binding.btnPlay.isVisible = true
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
        if (intent.resolveActivity(requireContext().packageManager) == null) {
            showToast(R.string.live_wallpaper_not_supported)
            return
        }
        runCatching { liveWallpaperLauncher.launch(intent) }
            .onFailure {
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

    override fun onPause() {
        stopVideoPreview()
        super.onPause()
    }

    override fun onDestroyView() {
        resultMessageHideAction?.let(binding.resultMessage::removeCallbacks)
        resultMessageHideAction = null
        binding.resultMessage.animate().cancel()
        binding.actionMenu.animate().cancel()
        stopVideoPreview()
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
