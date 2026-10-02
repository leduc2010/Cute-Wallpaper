package com.cute.wallpaper.ringtones.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.core.model.ContentRef
import com.cute.wallpaper.ringtones.data.fake.FakeContentDataSource
import com.cute.wallpaper.ringtones.data.fake.FakeQuickFilterKind
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    fakeContentDataSource: FakeContentDataSource,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _searchQuery = savedStateHandle.getLiveData("search_query", "")
    private val _searchFiltersVisible = savedStateHandle.getLiveData("search_filters_visible", false)
    private val _selectedColorId = savedStateHandle.getLiveData("selected_color_id", "")
    private val _selectedGenreIds = savedStateHandle.getLiveData("selected_genre_ids", arrayListOf<String>())
    private val _searchFilterOptions = MutableLiveData(
        SearchFilterUiModel(
            colors = fakeContentDataSource.colors.map {
                SearchColorUiModel(it.id, it.label, it.colorHex)
            },
            genres = fakeContentDataSource.genres.map {
                SearchGenreUiModel(it.id, it.label, it.emoji)
            },
            quickFilters = fakeContentDataSource.quickFilters.map {
                SearchQuickFilterUiModel(
                    id = it.id,
                    label = it.label,
                    emoji = it.emoji,
                    kind = when (it.kind) {
                        FakeQuickFilterKind.COLOR -> SearchQuickFilterKind.COLOR
                        FakeQuickFilterKind.GENRE -> SearchQuickFilterKind.GENRE
                    }
                )
            }
        )
    )
    private val _bottomContentPadding = MutableLiveData(0)
    private val _contentItems = MutableLiveData(fakeContentDataSource.contents.map { it.toUiModel() })

    val searchQuery: LiveData<String> = _searchQuery
    val searchFiltersVisible: LiveData<Boolean> = _searchFiltersVisible
    val selectedColorId: LiveData<String> = _selectedColorId
    val selectedGenreIds: LiveData<ArrayList<String>> = _selectedGenreIds
    val searchFilterOptions: LiveData<SearchFilterUiModel> = _searchFilterOptions
    val bottomContentPadding: LiveData<Int> = _bottomContentPadding
    val contentItems: LiveData<List<HomeContentUiModel>> = _contentItems
    val favoriteKeys: LiveData<Set<String>> = appPreferences.favoriteKeys.asLiveData()

    fun updateSearchQuery(query: String) {
        if (_searchQuery.value != query) _searchQuery.value = query
    }

    fun setSearchFiltersVisible(isVisible: Boolean) {
        if (_searchFiltersVisible.value != isVisible) _searchFiltersVisible.value = isVisible
    }

    fun setSelectedColor(colorId: String?) {
        val value = colorId.orEmpty()
        if (_selectedColorId.value != value) _selectedColorId.value = value
    }

    fun setGenreSelected(genreId: String, isSelected: Boolean) {
        val updated = _selectedGenreIds.value.orEmpty().toMutableSet()
        if (isSelected) updated += genreId else updated -= genreId
        val value = ArrayList(updated)
        if (_selectedGenreIds.value != value) _selectedGenreIds.value = value
    }

    fun updateBottomContentPadding(padding: Int) {
        if (_bottomContentPadding.value != padding) _bottomContentPadding.value = padding
    }

    fun setFavorite(content: ContentRef, isFavorite: Boolean) {
        viewModelScope.launch {
            appPreferences.setFavorite(content.toFavoriteKey(), isFavorite)
        }
    }

}
