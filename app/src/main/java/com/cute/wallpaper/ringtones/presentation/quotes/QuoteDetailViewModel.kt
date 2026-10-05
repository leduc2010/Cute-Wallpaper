package com.cute.wallpaper.ringtones.presentation.quotes

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.data.quote.QuoteImageExporter
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.presentation.home.toUiModel
import com.cute.wallpaper.ringtones.presentation.navigation.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface QuoteExportEvent {
    data object Saved : QuoteExportEvent
    data class ShareReady(val uri: Uri) : QuoteExportEvent
    data object Failed : QuoteExportEvent
}

@HiltViewModel
class QuoteDetailViewModel @Inject constructor(
    repository: ContentRepository,
    private val exporter: QuoteImageExporter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val item = repository.getContents().firstOrNull {
        it.type == ContentType.QUOTE && it.id == savedStateHandle.get<String>(ARG_CONTENT_ID)
    }?.toUiModel()
    private val _exporting = MutableLiveData(false)
    val exporting: LiveData<Boolean> = _exporting
    private val _event = MutableLiveData<Event<QuoteExportEvent>>()
    val event: LiveData<Event<QuoteExportEvent>> = _event

    /** Takes ownership of a private poster bitmap, including ignored duplicate requests. */
    fun save(bitmap: Bitmap) = export(bitmap, share = false)

    fun share(bitmap: Bitmap) = export(bitmap, share = true)

    private fun export(bitmap: Bitmap, share: Boolean) {
        if (_exporting.value == true || item?.quote.isNullOrBlank()) {
            bitmap.recycle()
            return
        }
        _exporting.value = true
        viewModelScope.launch {
            try {
                val result = if (share) QuoteExportEvent.ShareReady(exporter.share(bitmap))
                else if (exporter.save(bitmap) != null) QuoteExportEvent.Saved
                else QuoteExportEvent.Failed
                _event.value = Event(result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _event.value = Event(QuoteExportEvent.Failed)
            } finally {
                bitmap.recycle()
                _exporting.value = false
            }
        }
    }

    companion object {
        const val ARG_CONTENT_ID = "quote_content_id"
    }
}
