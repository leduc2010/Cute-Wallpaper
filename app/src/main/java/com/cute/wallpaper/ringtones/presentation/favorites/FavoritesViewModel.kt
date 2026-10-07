package com.cute.wallpaper.ringtones.presentation.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.data.media.MediaInfoResolver
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
    private val mediaInfoResolver: MediaInfoResolver,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val contents = contentRepository.getContents().map { it.toUiModel() }
    val favoriteKeys = appPreferences.favoriteKeys.asLiveData()
    private val selected = savedStateHandle.getLiveData("favorite_category", FavoriteCategory.WALLPAPER.name)
    val selectedCategory = selected
    private val query = savedStateHandle.getLiveData("favorite_query", "")
    val searchQuery = query
    val searchVisible = savedStateHandle.getLiveData("favorite_search_visible", false)

    private val mediaInfoRequests = mutableSetOf<String>()
    private val _durationByContentId = MutableLiveData<Map<String, Long>>(emptyMap())
    val durationByContentId: LiveData<Map<String, Long>> = _durationByContentId

    fun ensureVideoDuration(contentId: String, url: String?) {
        val source = url?.takeIf(String::isNotBlank) ?: return
        if (_durationByContentId.value.orEmpty().containsKey(contentId)) return
        if (!mediaInfoRequests.add(contentId)) return

        viewModelScope.launch {
            try {
                val duration = mediaInfoResolver.resolveDuration(source) ?: 0L
                if (duration > 0L) {
                    _durationByContentId.value =
                        _durationByContentId.value.orEmpty() + (contentId to duration)
                }
            } finally {
                mediaInfoRequests -= contentId
            }
        }
    }

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
