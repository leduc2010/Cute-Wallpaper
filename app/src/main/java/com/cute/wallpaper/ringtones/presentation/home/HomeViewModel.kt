package com.cute.wallpaper.ringtones.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.domain.model.ContentRef
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    contentRepository: ContentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val contents = contentRepository.getContents().map { it.toUiModel() }
    private val tab = MainTab.entries.firstOrNull {
        it.name == savedStateHandle.get<String>(ARG_MAIN_TAB)
    } ?: MainTab.WALLPAPERS
    private val filterContents = if (tab == MainTab.FAVORITES) {
        contents
    } else {
        contents.filter { it.type == tab.contentType }
    }
    private val _searchQuery = savedStateHandle.getLiveData("search_query", "")
    private val _searchFiltersVisible = savedStateHandle.getLiveData("search_filters_visible", false)
    private val _selectedColorId = savedStateHandle.getLiveData("selected_color_id", "")
    private val _selectedGenreIds = savedStateHandle.getLiveData("selected_genre_ids", arrayListOf<String>())
    private val availableColors = filterContents.mapNotNull { it.color }
        .distinct()
    private val availableTags = buildList {
        val seen = linkedSetOf<String>()
        filterContents.forEach { item ->
            item.tags.forEach { tag ->
                if (seen.add(tag)) add(tag)
            }
        }
    }
    private val _searchFilterOptions = MutableLiveData(
        SearchFilterUiModel(
            colors = availableColors.map { color ->
                SearchColorUiModel(
                    id = color,
                    label = color.replaceFirstChar(Char::titlecase),
                    colorHex = color.toDisplayColorHex()
                )
            },
            genres = availableTags.map { tag ->
                SearchGenreUiModel(
                    id = tag,
                    label = tag.replaceFirstChar(Char::titlecase),
                    emoji = ""
                )
            },
            quickFilters = availableTags.take(MAX_QUICK_FILTERS).map { tag ->
                SearchQuickFilterUiModel(
                    id = tag,
                    label = tag.replaceFirstChar(Char::titlecase),
                    emoji = "",
                    kind = SearchQuickFilterKind.GENRE
                )
            }
        )
    )
    private val _selectedRingtoneCategory = savedStateHandle.getLiveData(
        "ringtone_category",
        RingtoneCategory.RINGTONES
    )
    val selectedRingtoneCategory: LiveData<RingtoneCategory> = _selectedRingtoneCategory

    private val _bottomContentPadding = MutableLiveData(0)
    private val _contentItems = MutableLiveData(contents)

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

    fun selectRingtoneCategory(category: RingtoneCategory) {
        if (_selectedRingtoneCategory.value != category) {
            _selectedRingtoneCategory.value = category
        }
    }

    fun updateBottomContentPadding(padding: Int) {
        if (_bottomContentPadding.value != padding) _bottomContentPadding.value = padding
    }

    fun setFavorite(content: ContentRef, isFavorite: Boolean) {
        viewModelScope.launch {
            appPreferences.setFavorite(content.toFavoriteKey(), isFavorite)
        }
    }

    private companion object {
        const val ARG_MAIN_TAB = "main_tab"
        const val MAX_QUICK_FILTERS = 8
    }
}
