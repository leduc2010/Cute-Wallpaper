package com.cute.wallpaper.ringtones.presentation.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.domain.model.ContentRef
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.RingtoneCategory
import com.cute.wallpaper.ringtones.presentation.home.toDisplayColorHex
import com.cute.wallpaper.ringtones.presentation.home.toUiModel
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    contentRepository: ContentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val mode: SearchMode = enumValueOrDefault(savedStateHandle[ARG_MODE], SearchMode.KEYWORD)
    val tab: MainTab = enumValueOrDefault(savedStateHandle[ARG_MAIN_TAB], MainTab.WALLPAPERS)

    private val initialColorId: String = savedStateHandle.get<String>(ARG_COLOR_ID).orEmpty()
    private val initialGenreId: String = savedStateHandle.get<String>(ARG_GENRE_ID).orEmpty()
    private val _query = savedStateHandle.getLiveData(ARG_QUERY, "")
    private val contents = contentRepository.getContents().map { it.toUiModel() }
    private val _contentItems = MutableLiveData(contents)
    private val filterContents = if (tab == MainTab.FAVORITES) {
        contents
    } else {
        contents.filter { it.type == tab.contentType }
    }
    private val colors = filterContents.mapNotNull { it.color }.distinct()
    private val tags = buildList {
        val seen = linkedSetOf<String>()
        filterContents.forEach { item ->
            item.tags.forEach { tag -> if (seen.add(tag)) add(tag) }
        }
    }

    val pages: List<SearchPageUiModel> = if (tab == MainTab.RINGTONES) {
        RingtoneCategory.pages.map { category ->
            SearchPageUiModel(
                id = category.remoteKey,
                label = category.name.lowercase().replaceFirstChar(Char::titlecase),
                emoji = "",
                kind = SearchPageKind.CATEGORY
            )
        }
    } else when (mode) {
        SearchMode.COLOR -> colors.map { color ->
            SearchPageUiModel(
                id = color,
                label = color.replaceFirstChar(Char::titlecase),
                emoji = "",
                kind = SearchPageKind.COLOR,
                colorHex = color.toDisplayColorHex()
            )
        }

        SearchMode.GENRE, SearchMode.KEYWORD -> tags.map { tag ->
            SearchPageUiModel(
                id = tag,
                label = tag.replaceFirstChar(Char::titlecase),
                emoji = "",
                kind = SearchPageKind.GENRE
            )
        }
    }

    val initialPageIndex: Int = pages.indexOfFirst {
        when (mode) {
            SearchMode.COLOR -> it.id == initialColorId
            SearchMode.GENRE -> it.id == initialGenreId
            SearchMode.KEYWORD -> false
        }
    }.takeIf { it >= 0 } ?: 0

    private val _selectedPageIndex = savedStateHandle.getLiveData(
        SELECTED_PAGE_INDEX_KEY,
        initialPageIndex
    )
    val selectedPageIndex: LiveData<Int> = _selectedPageIndex
    val query: LiveData<String> = _query
    val contentItems: LiveData<List<HomeContentUiModel>> = _contentItems
    val favoriteKeys: LiveData<Set<String>> = appPreferences.favoriteKeys.asLiveData()

    fun updateQuery(value: String) {
        if (_query.value != value) _query.value = value
    }

    fun selectPage(position: Int) {
        if (position in pages.indices && _selectedPageIndex.value != position) {
            _selectedPageIndex.value = position
        }
    }

    fun filteredItems(page: SearchPageUiModel): List<HomeContentUiModel> {
        val queryValue = _query.value.orEmpty().trim()
        val favorites = favoriteKeys.value.orEmpty()
        return _contentItems.value.orEmpty().filter { item ->
            val matchesTab = if (tab == MainTab.FAVORITES) {
                item.ref.toFavoriteKey() in favorites
            } else {
                item.type == tab.contentType
            }
            val matchesPage = when (page.kind) {
                SearchPageKind.COLOR -> page.id == item.color
                SearchPageKind.GENRE -> page.id in item.tags
                SearchPageKind.CATEGORY -> page.id == item.category
            }
            matchesTab && matchesPage && item.matches(queryValue)
        }
    }

    fun setFavorite(content: ContentRef, isFavorite: Boolean) {
        viewModelScope.launch { appPreferences.setFavorite(content.toFavoriteKey(), isFavorite) }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T {
        return enumValues<T>().firstOrNull { it.name == value } ?: default
    }

    companion object {
        const val ARG_MODE = "search_mode"
        const val ARG_MAIN_TAB = "search_main_tab"
        const val ARG_QUERY = "search_query"
        const val ARG_COLOR_ID = "search_color_id"
        const val ARG_GENRE_ID = "search_genre_id"
        private const val SELECTED_PAGE_INDEX_KEY = "search_selected_page_index"
    }
}
