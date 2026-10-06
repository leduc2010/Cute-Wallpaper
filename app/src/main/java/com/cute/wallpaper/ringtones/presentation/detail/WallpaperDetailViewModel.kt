package com.cute.wallpaper.ringtones.presentation.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.domain.model.ContentRef
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.model.WallpaperTarget
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.domain.repository.DownloadedWallpaperStore
import com.cute.wallpaper.ringtones.domain.repository.WallpaperRepository
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import com.cute.wallpaper.ringtones.presentation.home.HomeContentUiModel
import com.cute.wallpaper.ringtones.presentation.home.toUiModel
import com.cute.wallpaper.ringtones.presentation.navigation.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DetailActionState { COLLAPSED, EXPANDED, LOADING }

sealed interface DetailActionResult {
    data class SetSuccess(
        val target: WallpaperTarget,
        val previewUrl: String
    ) : DetailActionResult

    data object SetFailed : DetailActionResult
    data object DownloadSuccess : DetailActionResult
    data object DownloadFailed : DetailActionResult
    data class ShareReady(val uri: String) : DetailActionResult
    data object ShareFailed : DetailActionResult
}

@HiltViewModel
class WallpaperDetailViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val wallpaperRepository: WallpaperRepository,
    private val downloadedWallpaperStore: DownloadedWallpaperStore,
    contentRepository: ContentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val selectedId = savedStateHandle.get<String>(ARG_CONTENT_ID).orEmpty()
    private val selectedType = runCatching {
        ContentType.valueOf(savedStateHandle.get<String>(ARG_CONTENT_TYPE).orEmpty())
    }.getOrDefault(ContentType.WALLPAPER)
    private val allItems = contentRepository.getContents().map { it.toUiModel() }
    private val selectedItem = allItems.firstOrNull {
        it.id == selectedId && it.type == selectedType
    }

    private val collectionItems: List<HomeContentUiModel> = allItems.filter { item ->
        item.type == selectedType &&
            !item.contentUrl.isNullOrBlank() &&
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
    private val _actionResult = MutableLiveData<Event<DetailActionResult>>()
    val actionResult: LiveData<Event<DetailActionResult>> = _actionResult

    fun toggleActions() {
        _actionState.value = when (_actionState.value) {
            DetailActionState.EXPANDED -> DetailActionState.COLLAPSED
            DetailActionState.LOADING -> DetailActionState.LOADING
            else -> DetailActionState.EXPANDED
        }
    }

    fun collapseActions() {
        if (_actionState.value == DetailActionState.EXPANDED) {
            _actionState.value = DetailActionState.COLLAPSED
        }
    }

    fun selectPage(position: Int) {
        if (position !in items.indices) return
        if (_currentPage.value != position) _currentPage.value = position
        if (_actionState.value == DetailActionState.EXPANDED) {
            _actionState.value = DetailActionState.COLLAPSED
        }
    }

    fun setWallpaper(target: WallpaperTarget) {
        val item = currentItem() ?: return
        val lockUrl = item.contentUrl ?: return
        val previewUrl = when (target) {
            WallpaperTarget.HOME -> item.secondaryContentUrl ?: lockUrl
            WallpaperTarget.LOCK -> lockUrl
            WallpaperTarget.BOTH -> item.thumbnailUrl ?: item.secondaryContentUrl ?: lockUrl
        }
        runAction(
            success = DetailActionResult.SetSuccess(target, previewUrl),
            failure = DetailActionResult.SetFailed
        ) {
            wallpaperRepository.setWallpaper(
                lockUrl = lockUrl,
                homeUrl = item.secondaryContentUrl,
                target = target
            )
        }
    }

    fun downloadWallpaper() {
        val item = currentItem()?.takeIf { it.downloadEnabled } ?: return
        val url = item.contentUrl ?: return
        runAction(
            success = DetailActionResult.DownloadSuccess,
            failure = DetailActionResult.DownloadFailed
        ) {
            val savedUri = wallpaperRepository.downloadWallpaper(
                url,
                downloadFileName(item)
            ) ?: return@runAction false
            downloadedWallpaperStore.add(
                key = item.ref.toFavoriteKey(),
                uri = savedUri
            )
            true
        }
    }

    fun shareWallpaper() {
        val item = currentItem() ?: return
        val url = item.contentUrl ?: return
        if (_actionState.value == DetailActionState.LOADING) return
        _actionState.value = DetailActionState.LOADING
        viewModelScope.launch {
            val uri = wallpaperRepository.prepareShareWallpaper(url, downloadFileName(item))
            _actionState.value = DetailActionState.COLLAPSED
            _actionResult.value = Event(
                uri?.let(DetailActionResult::ShareReady) ?: DetailActionResult.ShareFailed
            )
        }
    }

    private fun currentItem(): HomeContentUiModel? {
        val position = _currentPage.value ?: initialPageIndex
        return items.getOrNull(position)
    }

    private fun runAction(
        success: DetailActionResult,
        failure: DetailActionResult,
        action: suspend () -> Boolean
    ) {
        if (_actionState.value == DetailActionState.LOADING) return
        _actionState.value = DetailActionState.LOADING
        viewModelScope.launch {
            val completed = action()
            _actionState.value = DetailActionState.COLLAPSED
            _actionResult.value = Event(if (completed) success else failure)
        }
    }

    private fun downloadFileName(item: HomeContentUiModel): String {
        val extension = item.contentUrl.orEmpty()
            .substringBefore('?')
            .substringAfterLast('.', "jpg")
            .lowercase()
            .takeIf { it in SUPPORTED_IMAGE_EXTENSIONS }
            ?: "jpg"
        return "CuteWallpaper_${item.id.replace(':', '_')}.$extension"
    }

    fun setFavorite(content: ContentRef, isFavorite: Boolean) {
        viewModelScope.launch { appPreferences.setFavorite(content.toFavoriteKey(), isFavorite) }
    }

    companion object {
        const val ARG_CONTENT_ID = "detail_content_id"
        const val ARG_CONTENT_TYPE = "detail_content_type"
        private const val CURRENT_PAGE_KEY = "detail_current_page"
        private val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")
    }
}
