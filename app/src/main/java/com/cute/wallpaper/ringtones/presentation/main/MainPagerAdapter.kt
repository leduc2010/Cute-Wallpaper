package com.cute.wallpaper.ringtones.presentation.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cute.wallpaper.ringtones.presentation.home.HomeFragment

class MainPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = MainTab.entries.size
    override fun createFragment(position: Int): Fragment = HomeFragment.newInstance(MainTab.entries[position])
}
