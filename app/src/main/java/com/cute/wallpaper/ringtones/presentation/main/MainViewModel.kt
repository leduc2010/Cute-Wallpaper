package com.cute.wallpaper.ringtones.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.cute.wallpaper.ringtones.presentation.home.WallpaperCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _selectedTab = savedStateHandle.getLiveData("main_tab", MainTab.WALLPAPERS)
    val selectedTab: LiveData<MainTab> = _selectedTab

    private val _selectedCollection = savedStateHandle.getLiveData(
        "main_collection",
        WallpaperCollection.WALLPAPER
    )
    val selectedCollection: LiveData<WallpaperCollection> = _selectedCollection

    private val _bottomContentPadding = MutableLiveData(0)
    val bottomContentPadding: LiveData<Int> = _bottomContentPadding

    fun selectTab(tab: MainTab) {
        if (_selectedTab.value != tab) _selectedTab.value = tab
    }

    fun selectCollection(collection: WallpaperCollection) {
        if (_selectedCollection.value != collection) _selectedCollection.value = collection
    }

    fun updateBottomContentPadding(padding: Int) {
        if (_bottomContentPadding.value != padding) _bottomContentPadding.value = padding
    }
}
