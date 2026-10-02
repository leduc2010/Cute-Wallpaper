package com.cute.wallpaper.ringtones.presentation.search

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class SearchFilterPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val pages: List<SearchPageUiModel>
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        return SearchResultFragment.newInstance(pages[position])
    }
}
