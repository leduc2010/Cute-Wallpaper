package com.cute.wallpaper.ringtones.presentation.main

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
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
        items.forEach { item ->
            item.container.setOnClickListener {
                if (item.tab != selectedTab) {
                    onTabSelected?.invoke(item.tab)
                }
            }
        }

        renderSelection()
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

    }

    private data class NavItem(
        val container: LinearLayout,
        val label: android.widget.TextView,
        val tab: MainTab
    )
}
