package com.cute.wallpaper.ringtones.presentation.search

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
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.toUiModel
import com.cute.wallpaper.ringtones.presentation.main.MainTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    fakeContentDataSource: FakeContentDataSource,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val mode: SearchMode = enumValueOrDefault(savedStateHandle[ARG_MODE], SearchMode.KEYWORD)
    val tab: MainTab = enumValueOrDefault(savedStateHandle[ARG_MAIN_TAB], MainTab.WALLPAPERS)

    private val initialColorId: String = savedStateHandle.get<String>(ARG_COLOR_ID).orEmpty()
    private val initialGenreId: String = savedStateHandle.get<String>(ARG_GENRE_ID).orEmpty()
    private val _query = savedStateHandle.getLiveData(ARG_QUERY, "")
    private val _contentItems = MutableLiveData(fakeContentDataSource.contents.map { it.toUiModel() })

    val pages: List<SearchPageUiModel> = when (mode) {
        SearchMode.COLOR -> fakeContentDataSource.colors.map {
            SearchPageUiModel(
                id = it.id,
                label = it.label,
                emoji = "",
                kind = SearchPageKind.COLOR,
                colorHex = it.colorHex
            )
        }

        SearchMode.GENRE -> fakeContentDataSource.genres.map {
            SearchPageUiModel(
                id = it.id,
                label = it.label.replaceFirstChar { char -> char.uppercase() },
                emoji = it.emoji,
                kind = SearchPageKind.GENRE
            )
        }

        SearchMode.KEYWORD -> fakeContentDataSource.quickFilters.map {
            SearchPageUiModel(
                id = it.id,
                label = it.label,
                emoji = it.emoji,
                kind = when (it.kind) {
                    FakeQuickFilterKind.COLOR -> SearchPageKind.COLOR
                    FakeQuickFilterKind.GENRE -> SearchPageKind.GENRE
                }
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
                SearchPageKind.COLOR -> page.id in item.colorIds
                SearchPageKind.GENRE -> page.id in item.genreIds
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
