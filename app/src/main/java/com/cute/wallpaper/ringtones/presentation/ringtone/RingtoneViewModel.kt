package com.cute.wallpaper.ringtones.presentation.ringtone

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cute.wallpaper.ringtones.domain.model.ContentItem
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.model.RingtoneTarget
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.cute.wallpaper.ringtones.domain.repository.RingtoneRepository
import com.cute.wallpaper.ringtones.presentation.navigation.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RingtonePlaybackState(
    val contentId: String? = null,
    val isPreparing: Boolean = false,
    val isPlaying: Boolean = false,
    val progress: Int = 0
)

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
    private val ringtoneRepository: RingtoneRepository
) : ViewModel() {
    private val contents = contentRepository.getContents()
        .filter { it.type == ContentType.RINGTONE }
        .associateBy(ContentItem::id)

    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var pendingSet: Pair<String, RingtoneTarget>? = null
    private var isSetting = false

    private val _playbackState = MutableLiveData(RingtonePlaybackState())
    val playbackState: LiveData<RingtonePlaybackState> = _playbackState

    private val _actionEvent = MutableLiveData<Event<RingtoneActionEvent>>()
    val actionEvent: LiveData<Event<RingtoneActionEvent>> = _actionEvent

    private val progressUpdater = object : Runnable {
        override fun run() {
            val player = mediaPlayer ?: return
            val state = _playbackState.value ?: return
            if (state.contentId == null) return

            val duration = player.duration.takeIf { it > 0 } ?: 0
            val progress = if (duration > 0) {
                (player.currentPosition * 100 / duration).coerceIn(0, 100)
            } else {
                0
            }
            _playbackState.value = state.copy(
                isPlaying = player.isPlaying,
                progress = progress
            )
            if (player.isPlaying) handler.postDelayed(this, PROGRESS_INTERVAL_MS)
        }
    }

    fun togglePreview(contentId: String) {
        val content = contents[contentId] ?: return
        val url = content.contentUrl?.takeIf(String::isNotBlank) ?: return
        val state = _playbackState.value ?: RingtonePlaybackState()

        if (state.contentId == contentId && mediaPlayer != null && !state.isPreparing) {
            val player = mediaPlayer ?: return
            if (player.isPlaying) {
                player.pause()
                handler.removeCallbacks(progressUpdater)
                _playbackState.value = state.copy(isPlaying = false)
            } else {
                player.start()
                _playbackState.value = state.copy(isPlaying = true)
                scheduleProgress()
            }
            return
        }

        preparePreview(contentId, url)
    }

    fun stopPreview() {
        handler.removeCallbacks(progressUpdater)
        mediaPlayer?.runCatching { stop() }
        releasePlayer()
        _playbackState.value = RingtonePlaybackState()
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

    private fun preparePreview(contentId: String, url: String) {
        handler.removeCallbacks(progressUpdater)
        releasePlayer()
        _playbackState.value = RingtonePlaybackState(
            contentId = contentId,
            isPreparing = true
        )

        runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { player ->
                    if (_playbackState.value?.contentId != contentId) return@setOnPreparedListener
                    player.start()
                    _playbackState.value = RingtonePlaybackState(
                        contentId = contentId,
                        isPlaying = true
                    )
                    scheduleProgress()
                }
                setOnCompletionListener {
                    handler.removeCallbacks(progressUpdater)
                    runCatching { it.seekTo(0) }
                    _playbackState.value = RingtonePlaybackState(contentId = contentId)
                }
                setOnErrorListener { _, _, _ ->
                    handlePreviewError()
                    true
                }
                prepareAsync()
            }
        }.onSuccess {
            mediaPlayer = it
        }.onFailure {
            handlePreviewError()
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

    private fun scheduleProgress() {
        handler.removeCallbacks(progressUpdater)
        handler.post(progressUpdater)
    }

    private fun handlePreviewError() {
        handler.removeCallbacks(progressUpdater)
        releasePlayer()
        _playbackState.value = RingtonePlaybackState()
        _actionEvent.value = Event(RingtoneActionEvent.PreviewFailed)
    }

    private fun releasePlayer() {
        mediaPlayer?.runCatching { reset() }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onCleared() {
        stopPreview()
        super.onCleared()
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 250L
    }
}
