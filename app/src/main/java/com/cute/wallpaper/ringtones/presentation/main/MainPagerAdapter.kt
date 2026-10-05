package com.cute.wallpaper.ringtones.presentation.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cute.wallpaper.ringtones.presentation.home.HomeFragment
import com.cute.wallpaper.ringtones.presentation.profile.ProfilePicturesFragment
import com.cute.wallpaper.ringtones.presentation.quotes.QuotesFragment
import com.cute.wallpaper.ringtones.presentation.favorites.FavoritesFragment
import com.cute.wallpaper.ringtones.presentation.video.VideoWallpapersFragment

class MainPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = MainTab.entries.size
    override fun createFragment(position: Int): Fragment = when (val tab = MainTab.entries[position]) {
        MainTab.VIDEO_WALLPAPERS -> VideoWallpapersFragment()
        MainTab.PROFILE_PICTURES -> ProfilePicturesFragment()
        MainTab.QUOTES -> QuotesFragment()
        MainTab.FAVORITES -> FavoritesFragment()
        else -> HomeFragment.newInstance(tab)
    }
}
