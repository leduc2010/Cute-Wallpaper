package com.cute.wallpaper.ringtones.presentation.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.domain.model.ContentRef
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.toUiModel
import com.cute.wallpaper.ringtones.presentation.navigation.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LiveWallpaperEvent {
    data class LaunchPreview(val url: String) : LiveWallpaperEvent
    data object PrepareFailed : LiveWallpaperEvent
}

@HiltViewModel
class VideoWallpaperDetailViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    contentRepository: ContentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val selectedId = savedStateHandle.get<String>(ARG_CONTENT_ID).orEmpty()
    private val liveItems = contentRepository.getContents()
        .asSequence()
        .filter { it.type == ContentType.VIDEO_WALLPAPER && !it.contentUrl.isNullOrBlank() }
        .map { it.toUiModel() }
        .toList()
    private val selectedIndex = liveItems.indexOfFirst { it.id == selectedId }

    val items: List<HomeContentUiModel> = when {
        liveItems.size <= 1 -> liveItems
        selectedIndex == 0 -> listOf(liveItems.last()) + liveItems
        selectedIndex == liveItems.lastIndex -> liveItems + liveItems.first()
        else -> liveItems
    }
    val initialPageIndex: Int = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)

    private val _currentPage = savedStateHandle.getLiveData(CURRENT_PAGE_KEY, initialPageIndex)
    val currentPage: LiveData<Int> = _currentPage
    val favoriteKeys: LiveData<Set<String>> = appPreferences.favoriteKeys.asLiveData()

    private val _isPreparing = MutableLiveData(false)
    val isPreparing: LiveData<Boolean> = _isPreparing
    private val _event = MutableLiveData<Event<LiveWallpaperEvent>>()
    val event: LiveData<Event<LiveWallpaperEvent>> = _event

    fun selectPage(position: Int) {
        if (position in items.indices && _currentPage.value != position) {
            _currentPage.value = position
        }
    }

    fun prepareLiveWallpaper() {
        if (_isPreparing.value == true) return
        val url = currentItem()?.contentUrl?.takeIf(String::isNotBlank) ?: return
        _isPreparing.value = true
        viewModelScope.launch {
            val result = runCatching { appPreferences.setLiveWallpaperUrl(url) }
            _isPreparing.value = false
            _event.value = Event(
                if (result.isSuccess) LiveWallpaperEvent.LaunchPreview(url)
                else LiveWallpaperEvent.PrepareFailed
            )
        }
    }

    fun setFavorite(content: ContentRef, isFavorite: Boolean) {
        viewModelScope.launch { appPreferences.setFavorite(content.toFavoriteKey(), isFavorite) }
    }

    private fun currentItem(): HomeContentUiModel? {
        val position = _currentPage.value ?: initialPageIndex
        return items.getOrNull(position)
    }

    companion object {
        const val ARG_CONTENT_ID = "video_detail_content_id"
        private const val CURRENT_PAGE_KEY = "video_detail_current_page"
    }
}
