package com.cute.wallpaper.ringtones.presentation.ringtone

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RingtonePlaybackState(
    val contentId: String? = null,
    val isPreparing: Boolean = false,
    val isPlaying: Boolean = false,
    val progress: Int = 0,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L
)

sealed interface RingtonePreviewEvent {
    data object Failed : RingtonePreviewEvent
}

class RingtonePreviewPlayer @Inject constructor(
    @ApplicationContext context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var released = false

    private val _state = MutableStateFlow(RingtonePlaybackState())
    val state: StateFlow<RingtonePlaybackState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RingtonePreviewEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RingtonePreviewEvent> = _events.asSharedFlow()

    private val player = ExoPlayer.Builder(context).build()

    init {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            _state.update {
                                it.copy(
                                    isPreparing = false,
                                    isPlaying = player.isPlaying,
                                    progress = currentProgress(),
                                    currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                                    durationMs = currentDuration()
                                )
                            }
                        }

                        Player.STATE_ENDED -> {
                            stopProgressUpdates()
                            player.pause()
                            player.seekTo(0)
                            _state.update {
                                it.copy(
                                    isPreparing = false,
                                    isPlaying = false,
                                    progress = 0,
                                    currentPositionMs = 0L,
                                    durationMs = currentDuration()
                                )
                            }
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.update {
                        it.copy(
                            isPreparing = player.playbackState == Player.STATE_BUFFERING,
                            isPlaying = isPlaying,
                            progress = currentProgress(),
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = currentDuration()
                        )
                    }
                    if (isPlaying) {
                        startProgressUpdates()
                    } else {
                        stopProgressUpdates()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    stopProgressUpdates()
                    player.stop()
                    player.clearMediaItems()
                    _state.value = RingtonePlaybackState()
                    _events.tryEmit(RingtonePreviewEvent.Failed)
                }
            }
        )
    }

    fun play(contentId: String, url: String) {
        if (released) return

        stopProgressUpdates()
        _state.value = RingtonePlaybackState(
            contentId = contentId,
            isPreparing = true
        )

        player.apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    fun toggle() {
        if (released || _state.value.isPreparing || _state.value.contentId == null) return

        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0)
            }
            player.play()
        }
    }

    fun stop() {
        if (released) return

        stopProgressUpdates()
        player.stop()
        player.clearMediaItems()
        _state.value = RingtonePlaybackState()
    }

    fun release() {
        if (released) return

        released = true
        stopProgressUpdates()
        player.release()
        _state.value = RingtonePlaybackState()
        scope.cancel()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && player.isPlaying) {
                _state.update {
                    it.copy(
                        isPlaying = player.isPlaying,
                        progress = currentProgress(),
                        currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = currentDuration()
                    )
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun currentProgress(): Int {
        val duration = currentDuration().takeIf { it > 0 } ?: return 0
        return ((player.currentPosition * 100L) / duration)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun currentDuration(): Long =
        player.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L

    private companion object {
        const val PROGRESS_INTERVAL_MS = 250L
    }
}
