package com.cute.wallpaper.ringtones.presentation.detail

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import android.widget.Toast
import com.bumptech.glide.Glide
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentWallpaperDetailBinding
import com.cute.wallpaper.ringtones.domain.model.WallpaperTarget
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.search.SearchMode
import com.cute.wallpaper.ringtones.presentation.search.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.absoluteValue

@AndroidEntryPoint
class WallpaperDetailFragment : BaseFragment<FragmentWallpaperDetailBinding>() {
    private val viewModel: WallpaperDetailViewModel by viewModels()
    private var currentPosition = 0
    private var resultMessageHideAction: Runnable? = null

    private val downloadPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.downloadWallpaper()
        } else {
            Toast.makeText(
                requireContext(),
                R.string.wallpaper_download_permission_denied,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            currentPosition = position
            viewModel.selectPage(position)
            hideResultMessage()
            renderFavorite()
            renderItemActions()
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentWallpaperDetailBinding {
        return FragmentWallpaperDetailBinding.inflate(inflater, container, false)
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
                    putString(SearchViewModel.ARG_MAIN_TAB, MainTab.WALLPAPERS.name)
                    putString(SearchViewModel.ARG_QUERY, tag)
                    putString(SearchViewModel.ARG_GENRE_ID, tag)
                },
                NavOptions.Builder().setPopUpTo(R.id.wallpaperDetailFragment, true).build()
            )
        }
        WindowCompat.getInsetsController(requireActivity().window, binding.root)
            .isAppearanceLightStatusBars = true

        currentPosition = viewModel.currentPage.value
            ?.takeIf { it in viewModel.items.indices }
            ?: viewModel.initialPageIndex
        binding.wallpaperPager.apply {
            adapter = WallpaperDetailAdapter(
                items = viewModel.items,
                requestManager = Glide.with(this@WallpaperDetailFragment)
            )
            isSaveEnabled = false
            offscreenPageLimit = 3
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
        renderItemActions()
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener { navViewModel.back() }
        binding.primaryActionContainer.setOnClickListener { viewModel.toggleActions() }
        binding.btnPrimaryAction.setOnClickListener { viewModel.toggleActions() }
        binding.btnFavorite.setOnClickListener {
            val item = currentItem() ?: return@setOnClickListener
            val favorite = item.ref.toFavoriteKey() in viewModel.favoriteKeys.value.orEmpty()
            viewModel.setFavorite(item.ref, !favorite)
        }
        binding.btnInfo.setOnClickListener { showCurrentTags() }
        binding.actionSetWallpaper.setOnClickListener { viewModel.setWallpaper(WallpaperTarget.HOME) }
        binding.actionSetLock.setOnClickListener { viewModel.setWallpaper(WallpaperTarget.LOCK) }
        binding.actionSetBoth.setOnClickListener { viewModel.setWallpaper(WallpaperTarget.BOTH) }
        binding.actionDownload.setOnClickListener { downloadCurrentWallpaper() }
        binding.actionShare.setOnClickListener { viewModel.shareWallpaper() }
    }

    override fun observeData() {
        viewModel.favoriteKeys.observe(viewLifecycleOwner) { renderFavorite() }
        viewModel.actionState.observe(viewLifecycleOwner, ::renderActionState)
        viewModel.actionResult.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let(::showActionResult)
        }
    }

    private fun currentItem(): HomeContentUiModel? = viewModel.items.getOrNull(currentPosition)

    private fun renderItemActions() {
        val item = currentItem()
        binding.actionDownload.alpha = if (item?.downloadEnabled == true) 1f else 0.5f
        binding.actionDownload.isEnabled = item?.downloadEnabled == true &&
            viewModel.actionState.value != DetailActionState.LOADING
        binding.btnInfo.alpha = if (item?.tags?.isNotEmpty() == true) 1f else 0.5f
        binding.btnInfo.isEnabled = item?.tags?.isNotEmpty() == true &&
            viewModel.actionState.value != DetailActionState.LOADING
    }

    private fun renderFavorite() {
        val item = currentItem()
        binding.btnFavorite.isEnabled = item != null &&
            viewModel.actionState.value != DetailActionState.LOADING
        val favorite = item?.let {
            it.ref.toFavoriteKey() in viewModel.favoriteKeys.value.orEmpty()
        } == true
        binding.btnFavorite.setImageResource(
            if (favorite) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
    }

    private fun renderActionState(state: DetailActionState) {
        val expanded = state == DetailActionState.EXPANDED
        val loading = state == DetailActionState.LOADING
        animateActionMenu(expanded)
        binding.btnPrimaryAction.isVisible = !loading
        binding.actionProgress.isVisible = loading
        binding.wallpaperPager.isUserInputEnabled = !loading
        listOf(
            binding.btnFavorite,
            binding.actionSetWallpaper,
            binding.actionSetLock,
            binding.actionSetBoth,
            binding.btnInfo,
            binding.actionShare
        ).forEach { it.isEnabled = !loading }
        binding.primaryActionContainer.isEnabled = !loading
        renderItemActions()
        if (!loading) {
            binding.btnPrimaryAction.setImageResource(
                if (expanded) R.drawable.ic_close_circle else R.drawable.ic_set_wallpaper
            )
        }
    }

    private fun showActionResult(result: DetailActionResult) {
        when (result) {
            is DetailActionResult.SetSuccess -> showSetSuccess(result.target)

            DetailActionResult.SetFailed -> showToast(R.string.wallpaper_set_failed)
            DetailActionResult.DownloadSuccess -> showToast(R.string.wallpaper_download_success)
            DetailActionResult.DownloadFailed -> showToast(R.string.wallpaper_download_failed)
            is DetailActionResult.ShareReady -> shareWallpaper(Uri.parse(result.uri))
            DetailActionResult.ShareFailed -> showToast(R.string.wallpaper_share_failed)
        }
    }

    private fun showSetSuccess(target: WallpaperTarget) {
        val messageRes = when (target) {
            WallpaperTarget.HOME -> R.string.wallpaper_success_home
            WallpaperTarget.LOCK -> R.string.wallpaper_success_lock
            WallpaperTarget.BOTH -> R.string.wallpaper_success_both
        }
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

    private fun downloadCurrentWallpaper() {
        if (
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            downloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        viewModel.downloadWallpaper()
    }

    private fun shareWallpaper(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("wallpaper", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(
            Intent.createChooser(shareIntent, getString(R.string.wallpaper_share_chooser))
        )
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun animateActionMenu(show: Boolean) {
        val menu = binding.actionMenu
        menu.animate().cancel()
        if (show) {
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
                    if (viewModel.actionState.value != DetailActionState.EXPANDED) {
                        menu.isVisible = false
                        menu.alpha = 1f
                    }
                }
                .start()
        }
    }

    override fun onDestroyView() {
        resultMessageHideAction?.let(binding.resultMessage::removeCallbacks)
        resultMessageHideAction = null
        binding.resultMessage.animate().cancel()
        binding.actionMenu.animate().cancel()
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
