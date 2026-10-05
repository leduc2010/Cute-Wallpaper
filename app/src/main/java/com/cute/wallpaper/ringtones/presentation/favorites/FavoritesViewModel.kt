package com.cute.wallpaper.ringtones.presentation.favorites

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.domain.model.ContentRef
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.presentation.home.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    contentRepository: ContentRepository,
    private val appPreferences: AppPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val contents = contentRepository.getContents().map { it.toUiModel() }
    val favoriteKeys = appPreferences.favoriteKeys.asLiveData()
    private val selected = savedStateHandle.getLiveData("favorite_category", FavoriteCategory.WALLPAPER.name)
    val selectedCategory = selected
    private val query = savedStateHandle.getLiveData("favorite_query", "")
    val searchQuery = query
    val searchVisible = savedStateHandle.getLiveData("favorite_search_visible", false)

    fun selectCategory(category: FavoriteCategory) {
        if (selected.value != category.name) selected.value = category.name
    }

    fun updateQuery(value: String) {
        if (query.value != value) query.value = value
    }

    fun setFavorite(ref: ContentRef, favorite: Boolean) {
        viewModelScope.launch { appPreferences.setFavorite(ref.toFavoriteKey(), favorite) }
    }
}
