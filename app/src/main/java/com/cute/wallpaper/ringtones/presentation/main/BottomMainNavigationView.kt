package com.cute.wallpaper.ringtones.presentation.main

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.cute.wallpaper.ringtones.databinding.ViewBottomMainNavigationBinding

class BottomMainNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewBottomMainNavigationBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    private val items by lazy {
        listOf(
            NavItem(binding.itemWallpapers, binding.labelWallpapers, MainTab.WALLPAPERS),
            NavItem(
                binding.itemVideoWallpapers,
                binding.labelVideoWallpapers,
                MainTab.VIDEO_WALLPAPERS
            ),
            NavItem(binding.itemRingtones, binding.labelRingtones, MainTab.RINGTONES),
            NavItem(
                binding.itemProfilePictures,
                binding.labelProfilePictures,
                MainTab.PROFILE_PICTURES
            ),
            NavItem(binding.itemQuotes, binding.labelQuotes, MainTab.QUOTES),
            NavItem(binding.itemFavorites, binding.labelFavorites, MainTab.FAVORITES)
        )
    }

    private var selectedTab = MainTab.WALLPAPERS
    private var onTabSelected: ((MainTab) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        applyNavigationBarInsets()

        items.forEach { item ->
            item.container.setOnClickListener {
                if (item.tab != selectedTab) {
                    onTabSelected?.invoke(item.tab)
                }
            }
        }

        renderSelection()
    }

    private fun applyNavigationBarInsets() {
        val initialBottomPadding = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navigationBarBottom = insets
                .getInsets(WindowInsetsCompat.Type.navigationBars())
                .bottom
            view.updatePadding(bottom = initialBottomPadding + navigationBarBottom)
            insets
        }
    }

    fun setOnTabSelectedListener(listener: ((MainTab) -> Unit)?) {
        onTabSelected = listener
    }

    fun setSelectedTab(tab: MainTab) {
        if (selectedTab == tab) return
        selectedTab = tab
        renderSelection()
    }

    private fun renderSelection() {
        items.forEach { item ->
            val isSelected = item.tab == selectedTab
            item.container.isSelected = isSelected
            item.label.isVisible = isSelected
        }

        post {
            val selectedView = items.first { it.tab == selectedTab }.container
            val targetX = selectedView.left + selectedView.width / 2 - binding.navigationScroll.width / 2
            binding.navigationScroll.smoothScrollTo(targetX.coerceAtLeast(0), 0)
        }
    }

    private data class NavItem(
        val container: LinearLayout,
        val label: android.widget.TextView,
        val tab: MainTab
    )
}
