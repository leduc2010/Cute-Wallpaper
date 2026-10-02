package com.cute.wallpaper.ringtones.presentation.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cute.wallpaper.ringtones.presentation.home.demo.DemoCollection
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import com.cute.wallpaper.ringtones.presentation.main.hasCollections

class HomeCollectionPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val tab: MainTab
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = if (tab.hasCollections) DemoCollection.pages.size else 1

    override fun createFragment(position: Int): Fragment {
        val collection = if (tab.hasCollections) {
            DemoCollection.pages[position]
        } else {
            DemoCollection.WALLPAPER
        }
        return HomeCollectionFragment.newInstance(tab, collection)
    }
}
