package com.cute.wallpaper.ringtones.presentation.video

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.domain.model.ContentRef
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoWallpapersViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    contentRepository: ContentRepository
) : ViewModel() {

    val items: List<HomeContentUiModel> = contentRepository.getContents()
        .asSequence()
        .filter { it.type == ContentType.VIDEO_WALLPAPER && !it.contentUrl.isNullOrBlank() }
        .sortedBy { it.rank }
        .map { it.toUiModel() }
        .toList()

    val favoriteKeys: LiveData<Set<String>> = appPreferences.favoriteKeys.asLiveData()

    fun setFavorite(content: ContentRef, isFavorite: Boolean) {
        viewModelScope.launch {
            appPreferences.setFavorite(content.toFavoriteKey(), isFavorite)
        }
    }
}
