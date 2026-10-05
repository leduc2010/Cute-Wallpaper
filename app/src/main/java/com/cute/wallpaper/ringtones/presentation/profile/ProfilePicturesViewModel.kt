package com.cute.wallpaper.ringtones.presentation.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSearchState(
    val query: String = "",
    val filtersVisible: Boolean = false,
    val color: String = "",
    val tags: Set<String> = emptySet()
) {
    val isSearching: Boolean get() = filtersVisible || query.isNotBlank() || color.isNotBlank() || tags.isNotEmpty()
}

internal fun filterProfilePictures(
    contents: List<HomeContentUiModel>, state: ProfileSearchState
): List<HomeContentUiModel> = contents.filter { item ->
    item.type == ContentType.PROFILE_PICTURE && item.matches(state.query.trim()) &&
        (state.color.isBlank() || state.color.equals(item.color, ignoreCase = true)) &&
        (state.tags.isEmpty() || state.tags.any { selected ->
            item.tags.any { it.equals(selected, ignoreCase = true) }
        })
}

@HiltViewModel
class ProfilePicturesViewModel @Inject constructor(
    repository: ContentRepository,
    private val preferences: AppPreferences,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val contents = repository.getContents()
        .filter { it.type == ContentType.PROFILE_PICTURE }
        .sortedBy { it.rank }
        .map { it.toUiModel() }
    val availableTags: List<String> = contents.flatMap { it.tags }.distinctBy { it.lowercase() }
    private val _searchState = MutableLiveData(readSearchState())
    val searchState: LiveData<ProfileSearchState> = _searchState
    val favoriteKeys: LiveData<Set<String>> = preferences.favoriteKeys.asLiveData()

    fun visibleContents(): List<HomeContentUiModel> = filterProfilePictures(contents, _searchState.value ?: ProfileSearchState())

    fun updateQuery(query: String) {
        if (_searchState.value?.query == query) return
        savedState[QUERY] = query
        publishSearchState()
    }

    fun showFilters(show: Boolean) {
        if (_searchState.value?.filtersVisible == show) return
        savedState[FILTERS] = show
        publishSearchState()
    }

    fun selectColor(color: String?) {
        savedState[COLOR] = color.orEmpty()
        publishSearchState()
    }

    fun selectTag(tag: String, selected: Boolean) {
        val tags = _searchState.value?.tags.orEmpty().toMutableSet()
        if (selected) tags.add(tag) else tags.remove(tag)
        savedState[TAGS] = ArrayList(tags)
        publishSearchState()
    }

    fun setFavorite(item: HomeContentUiModel, favorite: Boolean) {
        viewModelScope.launch { preferences.setFavorite(item.ref.toFavoriteKey(), favorite) }
    }

    private fun publishSearchState() { _searchState.value = readSearchState() }
    private fun readSearchState() = ProfileSearchState(
        query = savedState.get<String>(QUERY).orEmpty(),
        filtersVisible = savedState[FILTERS] ?: false,
        color = savedState.get<String>(COLOR).orEmpty(),
        tags = savedState.get<ArrayList<String>>(TAGS).orEmpty().toSet()
    )

    private companion object {
        const val QUERY = "profile_search_query"
        const val FILTERS = "profile_search_filters"
        const val COLOR = "profile_search_color"
        const val TAGS = "profile_search_tags"
    }
}
