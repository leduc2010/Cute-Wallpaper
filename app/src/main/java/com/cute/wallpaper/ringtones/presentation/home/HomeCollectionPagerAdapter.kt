package com.cute.wallpaper.ringtones.presentation.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.main.hasCollections

class HomeCollectionPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val tab: MainTab
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = when {
        tab.hasCollections -> WallpaperCollection.pages.size
        tab == MainTab.RINGTONES -> RingtoneCategory.pages.size
        else -> 1
    }

    override fun createFragment(position: Int): Fragment {
        val collection = if (tab.hasCollections) {
            WallpaperCollection.pages[position]
        } else {
            WallpaperCollection.WALLPAPER
        }
        val ringtoneCategory = if (tab == MainTab.RINGTONES) {
            RingtoneCategory.pages[position]
        } else {
            RingtoneCategory.RINGTONES
        }
        return HomeCollectionFragment.newInstance(tab, collection, ringtoneCategory)
    }
}
