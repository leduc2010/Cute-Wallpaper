package com.cute.wallpaper.ringtones.presentation.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.core.model.ContentRef
import com.cute.wallpaper.ringtones.core.model.ContentType
import com.cute.wallpaper.ringtones.data.fake.FakeContentDataSource
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DetailActionState { COLLAPSED, EXPANDED, LOADING }

@HiltViewModel
class WallpaperDetailViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    fakeContentDataSource: FakeContentDataSource,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val selectedId = savedStateHandle.get<String>(ARG_CONTENT_ID).orEmpty()
    private val selectedType = runCatching {
        ContentType.valueOf(savedStateHandle.get<String>(ARG_CONTENT_TYPE).orEmpty())
    }.getOrDefault(ContentType.WALLPAPER)
    private val allItems = fakeContentDataSource.contents.map { it.toUiModel() }
    private val selectedItem = allItems.firstOrNull {
        it.id == selectedId && it.type == selectedType
    }

    private val collectionItems: List<HomeContentUiModel> = allItems.filter { item ->
        item.type == selectedType &&
            item.artworkIndex != null &&
            (selectedItem == null || item.collection == selectedItem.collection)
    }
    private val selectedCollectionIndex = collectionItems.indexOfFirst { it.id == selectedId }
    val items: List<HomeContentUiModel> = when {
        collectionItems.size <= 1 -> collectionItems
        selectedCollectionIndex == 0 -> listOf(collectionItems.last()) + collectionItems
        selectedCollectionIndex == collectionItems.lastIndex -> collectionItems + collectionItems.first()
        else -> collectionItems
    }
    val initialPageIndex: Int = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    private val _currentPage = savedStateHandle.getLiveData(CURRENT_PAGE_KEY, initialPageIndex)
    val currentPage: LiveData<Int> = _currentPage
    val favoriteKeys: LiveData<Set<String>> = appPreferences.favoriteKeys.asLiveData()

    private val _actionState = MutableLiveData(DetailActionState.COLLAPSED)
    val actionState: LiveData<DetailActionState> = _actionState
    private var actionJob: Job? = null

    fun toggleActions() {
        _actionState.value = when (_actionState.value) {
            DetailActionState.EXPANDED -> DetailActionState.COLLAPSED
            DetailActionState.LOADING -> DetailActionState.LOADING
            else -> DetailActionState.EXPANDED
        }
    }

    fun selectPage(position: Int) {
        if (position !in items.indices) return
        if (_currentPage.value != position) _currentPage.value = position
        actionJob?.cancel()
        actionJob = null
        if (_actionState.value != DetailActionState.COLLAPSED) {
            _actionState.value = DetailActionState.COLLAPSED
        }
    }

    fun startAction() {
        if (_actionState.value == DetailActionState.LOADING) return
        _actionState.value = DetailActionState.LOADING
        actionJob = viewModelScope.launch {
            delay(FAKE_ACTION_DURATION)
            _actionState.value = DetailActionState.COLLAPSED
            actionJob = null
        }
    }

    fun setFavorite(content: ContentRef, isFavorite: Boolean) {
        viewModelScope.launch { appPreferences.setFavorite(content.toFavoriteKey(), isFavorite) }
    }

    companion object {
        const val ARG_CONTENT_ID = "detail_content_id"
        const val ARG_CONTENT_TYPE = "detail_content_type"
        private const val CURRENT_PAGE_KEY = "detail_current_page"
        private const val FAKE_ACTION_DURATION = 900L
    }
}
