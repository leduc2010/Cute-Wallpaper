package com.cute.wallpaper.ringtones.presentation.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.cute.wallpaper.ringtones.data.fake.FakeContentDataSource
import com.cute.wallpaper.ringtones.databinding.FragmentWallpaperDetailBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.demo.DemoArtwork
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.absoluteValue

@AndroidEntryPoint
class WallpaperDetailFragment : BaseFragment<FragmentWallpaperDetailBinding>() {
    @Inject lateinit var fakeContentDataSource: FakeContentDataSource

    private val viewModel: WallpaperDetailViewModel by viewModels()
    private var currentPosition = 0

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            currentPosition = position
            viewModel.selectPage(position)
            renderFavorite()
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentWallpaperDetailBinding {
        return FragmentWallpaperDetailBinding.inflate(inflater, container, false)
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

        val artwork = DemoArtwork(resources, fakeContentDataSource.artworkRegions)
        currentPosition = viewModel.currentPage.value
            ?.takeIf { it in viewModel.items.indices }
            ?: viewModel.initialPageIndex
        binding.wallpaperPager.apply {
            adapter = WallpaperDetailAdapter(artwork, viewModel.items)
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
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener { navViewModel.back() }
        binding.btnPrimaryAction.setOnClickListener { viewModel.toggleActions() }
        binding.btnFavorite.setOnClickListener {
            val item = currentItem() ?: return@setOnClickListener
            val favorite = item.ref.toFavoriteKey() in viewModel.favoriteKeys.value.orEmpty()
            viewModel.setFavorite(item.ref, !favorite)
        }
        binding.btnInfo.setOnClickListener { viewModel.startAction() }
        listOf(
            binding.actionPhone,
            binding.actionLock,
            binding.actionPhoneLock,
            binding.actionDownload,
            binding.actionShare,
            binding.actionSafe
        ).forEach { action ->
            action.setOnClickListener { viewModel.startAction() }
        }
    }

    override fun observeData() {
        viewModel.favoriteKeys.observe(viewLifecycleOwner) { renderFavorite() }
        viewModel.actionState.observe(viewLifecycleOwner, ::renderActionState)
    }

    private fun currentItem(): HomeContentUiModel? = viewModel.items.getOrNull(currentPosition)

    private fun renderFavorite() {
        val item = currentItem()
        binding.btnFavorite.isEnabled = item != null
        val favorite = item?.let {
            it.ref.toFavoriteKey() in viewModel.favoriteKeys.value.orEmpty()
        } == true
        binding.btnFavorite.setImageResource(
            if (favorite) R.drawable.ic_home_heart_filled
            else R.drawable.ic_home_heart_outline
        )
    }

    private fun renderActionState(state: DetailActionState) {
        val expanded = state == DetailActionState.EXPANDED
        val loading = state == DetailActionState.LOADING
        animateActionMenu(expanded)
        binding.btnPrimaryAction.isVisible = !loading
        binding.actionProgress.isVisible = loading
        if (!loading) {
            binding.btnPrimaryAction.setImageResource(
                if (expanded) R.drawable.ic_detail_close else R.drawable.ic_detail_magic
            )
        }
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
        binding.actionMenu.animate().cancel()
        binding.wallpaperPager.unregisterOnPageChangeCallback(pageCallback)
        binding.wallpaperPager.adapter = null
        super.onDestroyView()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val ACTION_ANIMATION_DURATION = 180L
    }
}
