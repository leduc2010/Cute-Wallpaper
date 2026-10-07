package com.cute.wallpaper.ringtones.presentation.ringtone

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.data.media.MediaInfoResolver
import com.cute.wallpaper.ringtones.domain.model.ContentItem
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.model.RingtoneTarget
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.domain.repository.RingtoneRepository
import com.cute.wallpaper.ringtones.presentation.navigation.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RingtoneActionEvent {
    data object RequestWriteSettings : RingtoneActionEvent
    data object RequestStoragePermission : RingtoneActionEvent
    data class SetSuccess(
        val target: RingtoneTarget,
        val title: String
    ) : RingtoneActionEvent
    data object SetFailed : RingtoneActionEvent
    data object PreviewFailed : RingtoneActionEvent
}

@HiltViewModel
class RingtoneViewModel @Inject constructor(
    contentRepository: ContentRepository,
    private val ringtoneRepository: RingtoneRepository,
    private val previewPlayer: RingtonePreviewPlayer,
    private val mediaInfoResolver: MediaInfoResolver
) : ViewModel() {
    private val contents = contentRepository.getContents()
        .filter { it.type == ContentType.RINGTONE }
        .associateBy(ContentItem::id)

    private var pendingSet: Pair<String, RingtoneTarget>? = null
    private var isSetting = false
    private val durationRequests = mutableSetOf<String>()

    val playbackState: LiveData<RingtonePlaybackState> = previewPlayer.state.asLiveData()

    private val _durationByContentId = MutableLiveData<Map<String, Long>>(emptyMap())
    val durationByContentId: LiveData<Map<String, Long>> = _durationByContentId

    private val _actionEvent = MutableLiveData<Event<RingtoneActionEvent>>()
    val actionEvent: LiveData<Event<RingtoneActionEvent>> = _actionEvent

    init {
        viewModelScope.launch {
            previewPlayer.events.collect { event ->
                when (event) {
                    RingtonePreviewEvent.Failed -> {
                        _actionEvent.value = Event(RingtoneActionEvent.PreviewFailed)
                    }
                }
            }
        }
    }

    fun togglePreview(contentId: String) {
        val content = contents[contentId] ?: return
        val url = content.contentUrl?.takeIf(String::isNotBlank) ?: return
        val state = previewPlayer.state.value

        if (state.contentId == contentId && !state.isPreparing) {
            previewPlayer.toggle()
        } else {
            previewPlayer.play(contentId, url)
        }
    }

    fun stopPreview() {
        previewPlayer.stop()
    }

    fun ensureDuration(contentId: String) {
        if (_durationByContentId.value.orEmpty().containsKey(contentId)) return
        if (!durationRequests.add(contentId)) return

        val url = contents[contentId]?.contentUrl?.takeIf(String::isNotBlank)
        if (url == null) {
            durationRequests -= contentId
            return
        }

        viewModelScope.launch {
            val duration = mediaInfoResolver.resolveDuration(url).orZero()
            durationRequests -= contentId
            if (duration > 0) {
                _durationByContentId.value =
                    _durationByContentId.value.orEmpty() + (contentId to duration)
            }
        }
    }

    fun requestSet(contentId: String, target: RingtoneTarget) {
        if (isSetting || contents[contentId] == null) return
        pendingSet = contentId to target
        when {
            !ringtoneRepository.canWriteSystemSettings() -> {
                _actionEvent.value = Event(RingtoneActionEvent.RequestWriteSettings)
            }

            !ringtoneRepository.hasLegacyStoragePermission() -> {
                _actionEvent.value = Event(RingtoneActionEvent.RequestStoragePermission)
            }

            else -> performPendingSet()
        }
    }

    fun retryPendingSet() {
        if (pendingSet == null || isSetting) return
        when {
            !ringtoneRepository.canWriteSystemSettings() -> {
                pendingSet = null
                _actionEvent.value = Event(RingtoneActionEvent.SetFailed)
            }

            !ringtoneRepository.hasLegacyStoragePermission() -> {
                _actionEvent.value = Event(RingtoneActionEvent.RequestStoragePermission)
            }

            else -> performPendingSet()
        }
    }

    private fun performPendingSet() {
        val (contentId, target) = pendingSet ?: return
        val content = contents[contentId] ?: return
        val url = content.contentUrl?.takeIf(String::isNotBlank) ?: return
        isSetting = true

        viewModelScope.launch {
            val success = ringtoneRepository.setSystemSound(
                url = url,
                title = content.title,
                fileName = "CuteWallpaper_${content.id.replace(':', '_')}.mp3",
                target = target
            )
            isSetting = false
            pendingSet = null
            _actionEvent.value = Event(
                if (success) {
                    RingtoneActionEvent.SetSuccess(target, content.title)
                } else {
                    RingtoneActionEvent.SetFailed
                }
            )
        }
    }

    override fun onCleared() {
        previewPlayer.release()
        super.onCleared()
    }

    private fun Long?.orZero(): Long = this ?: 0L
}
