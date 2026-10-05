package com.cute.wallpaper.ringtones.presentation.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.cute.wallpaper.ringtones.R
import com.cute.wallpaper.ringtones.databinding.FragmentMainBinding
import com.cute.wallpaper.ringtones.presentation.base.BaseFragment
import com.cute.wallpaper.ringtones.presentation.home.WallpaperCollection
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : BaseFragment<FragmentMainBinding>() {
    private val viewModel: MainViewModel by viewModels()
    private var lastRenderedTab: MainTab? = null
    private var bottomNavBehavior: HideBottomViewOnScrollBehavior<BottomMainNavigationView>? = null
    private var appBarOffset = 0

    // The app bar consumes the first part of a drag, before the feed scrolls.
    private val appBarOffsetListener = AppBarLayout.OnOffsetChangedListener { _, offset ->
        when {
            offset < appBarOffset -> bottomNavBehavior?.slideDown(binding.bottomNav)
            offset > appBarOffset -> bottomNavBehavior?.slideUp(binding.bottomNav)
        }
        appBarOffset = offset
    }

    private val pagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            viewModel.selectTab(MainTab.entries[position])
        }
    }

    private val bottomLayoutListener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
        viewModel.updateBottomContentPadding(view.height)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMainBinding {
        return FragmentMainBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        WindowCompat.getInsetsController(requireActivity().window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            requireActivity().window.isNavigationBarContrastEnforced = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val safeArea = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(top = safeArea.top, left = safeArea.left, right = safeArea.right)
            insets
        }
        binding.contentPager.apply {
            adapter = MainPagerAdapter(this@MainFragment)
            isUserInputEnabled = false
            setCurrentItem((viewModel.selectedTab.value ?: MainTab.WALLPAPERS).ordinal, false)
            registerOnPageChangeCallback(pagerCallback)
        }
        bottomNavBehavior = HideBottomViewOnScrollBehavior<BottomMainNavigationView>().also {
            (binding.bottomNav.layoutParams as CoordinatorLayout.LayoutParams).behavior = it
        }
        binding.appBar.addOnOffsetChangedListener(appBarOffsetListener)
        binding.bottomNav.addOnLayoutChangeListener(bottomLayoutListener)
        binding.collections.categoryWallpaperImage.setImageResource(R.drawable.img_collection_wallpaper)
        binding.collections.categoryDualWallpapersImage.setImageResource(R.drawable.img_collection_dual_wallpapers)
        binding.collections.categoryBestImage.setImageResource(R.drawable.img_collection_best)
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun initListener() {
        binding.bottomNav.setOnTabSelectedListener(viewModel::selectTab)
        binding.header.btnVideo.setOnClickListener { viewModel.selectTab(MainTab.VIDEO_WALLPAPERS) }
        binding.collections.categoryWallpaper.setOnClickListener {
            viewModel.selectCollection(WallpaperCollection.WALLPAPER)
        }
        binding.collections.categoryDualWallpapers.setOnClickListener {
            viewModel.selectCollection(WallpaperCollection.DUAL_WALLPAPERS)
        }
        binding.collections.categoryBest.setOnClickListener {
            viewModel.selectCollection(WallpaperCollection.BEST)
        }
    }

    override fun observeData() {
        viewModel.selectedTab.observe(viewLifecycleOwner) { tab ->
            binding.header.tvTitle.setText(tab.titleRes)
            val showBrand = tab != MainTab.FAVORITES
            binding.header.brandImage.isVisible = showBrand
            binding.header.brandDivider.isVisible = showBrand
            binding.header.headerTitleContainer.isVisible = showBrand
            binding.header.favoritesHeaderSpacer.isVisible = !showBrand
            binding.collections.root.isVisible = tab.hasCollections
            binding.bottomNav.setSelectedTab(tab)
            if (binding.contentPager.currentItem != tab.ordinal) binding.contentPager.setCurrentItem(tab.ordinal, false)
            if (lastRenderedTab != null && lastRenderedTab != tab) {
                revealChrome(animate = false)
            }
            lastRenderedTab = tab
        }
        viewModel.selectedCollection.observe(viewLifecycleOwner) { collection ->
            val items = listOf(
                Triple(
                    binding.collections.categoryWallpaper,
                    binding.collections.categoryWallpaperLabel,
                    WallpaperCollection.WALLPAPER
                ),
                Triple(
                    binding.collections.categoryDualWallpapers,
                    binding.collections.categoryDualWallpapersLabel,
                    WallpaperCollection.DUAL_WALLPAPERS
                ),
                Triple(
                    binding.collections.categoryBest,
                    binding.collections.categoryBestLabel,
                    WallpaperCollection.BEST
                )
            )
            items.forEach { (item, label, value) ->
                val selected = collection == value
                item.isSelected = selected
                label.isVisible = selected
                item.elevation = if (selected) dp(2).toFloat() else 0f
            }
        }
    }

    fun revealChrome(animate: Boolean = true) {
        binding.appBar.setExpanded(true, animate)
        bottomNavBehavior?.slideUp(binding.bottomNav, animate)
    }

    override fun onDestroyView() {
        binding.contentPager.unregisterOnPageChangeCallback(pagerCallback)
        binding.contentPager.adapter = null
        binding.appBar.removeOnOffsetChangedListener(appBarOffsetListener)
        binding.bottomNav.removeOnLayoutChangeListener(bottomLayoutListener)
        binding.bottomNav.setOnTabSelectedListener(null)
        binding.bottomNav.animate().cancel()
        bottomNavBehavior = null
        lastRenderedTab = null
        appBarOffset = 0
        super.onDestroyView()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
