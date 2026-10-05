package com.cute.wallpaper.ringtones.presentation.quotes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.model.QuoteCategory
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QuotesTab { NEW, POPULAR, CATEGORIES }

data class QuotesState(
    val tab: QuotesTab = QuotesTab.NEW,
    val query: String = "",
    val categoryId: Int? = null
)

data class QuoteCategoryUiModel(
    val id: Int,
    val name: String,
    val thumbnailUrl: String?
)

internal fun filterQuotes(contents: List<HomeContentUiModel>, state: QuotesState): List<HomeContentUiModel> =
    contents.filter { item ->
        item.type == ContentType.QUOTE && !item.quote.isNullOrBlank() &&
            item.category.equals(when (state.tab) {
                QuotesTab.NEW -> "new"
                QuotesTab.POPULAR -> "popular"
                QuotesTab.CATEGORIES -> "category"
            }, ignoreCase = true) &&
            (state.categoryId == null || item.quoteCategoryId == state.categoryId) &&
            item.matches(state.query.trim())
    }

internal fun quoteCategories(categories: List<QuoteCategory>): List<QuoteCategoryUiModel> = categories.map { category ->
    QuoteCategoryUiModel(category.id, category.name, category.thumbnailUrl)
}

@HiltViewModel
class QuotesViewModel @Inject constructor(
    repository: ContentRepository,
    private val preferences: AppPreferences,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val contents = repository.getContents().map { it.toUiModel() }
    private val categories = repository.getQuoteCategories()
    private val _state = MutableLiveData(readState())
    val state: LiveData<QuotesState> = _state
    val favoriteKeys = preferences.favoriteKeys.asLiveData()

    fun visibleQuotes() = filterQuotes(contents, _state.value ?: QuotesState())
    fun visibleCategories(): List<QuoteCategoryUiModel> = quoteCategories(categories).filter {
        it.name.contains(_state.value?.query.orEmpty().trim(), ignoreCase = true)
    }

    fun updateQuery(query: String) {
        if (_state.value?.query == query) return
        savedState[QUERY] = query
        publish()
    }

    fun selectTab(tab: QuotesTab) {
        savedState[TAB] = tab.name
        savedState[CATEGORY_ID] = null
        savedState[QUERY] = ""
        publish()
    }

    fun selectCategory(category: QuoteCategoryUiModel) {
        savedState[TAB] = QuotesTab.CATEGORIES.name
        savedState[CATEGORY_ID] = category.id
        savedState[QUERY] = category.name
        publish()
    }

    fun showCategories() = selectTab(QuotesTab.CATEGORIES)

    fun setFavorite(item: HomeContentUiModel, favorite: Boolean) {
        viewModelScope.launch { preferences.setFavorite(item.ref.toFavoriteKey(), favorite) }
    }

    private fun publish() { _state.value = readState() }
    private fun readState() = QuotesState(
        tab = QuotesTab.entries.firstOrNull { it.name == savedState.get<String>(TAB) } ?: QuotesTab.NEW,
        query = savedState.get<String>(QUERY).orEmpty(),
        categoryId = savedState.get<Int>(CATEGORY_ID)
    )

    private companion object {
        const val TAB = "quotes_tab"
        const val QUERY = "quotes_query"
        const val CATEGORY_ID = "quotes_category_id"
    }
}
