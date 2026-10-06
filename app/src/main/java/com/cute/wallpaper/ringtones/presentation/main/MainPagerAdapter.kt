package com.cute.wallpaper.ringtones.presentation.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cute.wallpaper.ringtones.presentation.favorites.TabFavoritesFragment
import com.cute.wallpaper.ringtones.presentation.home.TabHomeFragment
import com.cute.wallpaper.ringtones.presentation.home.TabRingtonesFragment
import com.cute.wallpaper.ringtones.presentation.profile.TabProfilePicturesFragment
import com.cute.wallpaper.ringtones.presentation.quotes.TabQuotesFragment
import com.cute.wallpaper.ringtones.presentation.video.TabVideoWallpapersFragment

class MainPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = MainTab.entries.size

    override fun createFragment(position: Int): Fragment = when (MainTab.entries[position]) {
        MainTab.WALLPAPERS -> TabHomeFragment()
        MainTab.VIDEO_WALLPAPERS -> TabVideoWallpapersFragment()
        MainTab.RINGTONES -> TabRingtonesFragment.newInstance()
        MainTab.PROFILE_PICTURES -> TabProfilePicturesFragment()
        MainTab.QUOTES -> TabQuotesFragment()
        MainTab.FAVORITES -> TabFavoritesFragment()
    }
}
