package com.cute.wallpaper.ringtones.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.cute.wallpaper.ringtones.presentation.home.demo.DemoCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _selectedTab = savedStateHandle.getLiveData("main_tab", MainTab.WALLPAPERS)
    val selectedTab: LiveData<MainTab> = _selectedTab

    private val _selectedCollection = savedStateHandle.getLiveData(
        "main_collection",
        DemoCollection.WALLPAPER
    )
    val selectedCollection: LiveData<DemoCollection> = _selectedCollection

    private val _bottomContentPadding = MutableLiveData(0)
    val bottomContentPadding: LiveData<Int> = _bottomContentPadding

    init {
        _selectedCollection.value = _selectedCollection.value?.canonical()
            ?: DemoCollection.WALLPAPER
    }

    fun selectTab(tab: MainTab) {
        if (_selectedTab.value != tab) _selectedTab.value = tab
    }

    fun selectCollection(collection: DemoCollection) {
        val canonical = collection.canonical()
        if (_selectedCollection.value != canonical) _selectedCollection.value = canonical
    }

    fun updateBottomContentPadding(padding: Int) {
        if (_bottomContentPadding.value != padding) _bottomContentPadding.value = padding
    }
}
