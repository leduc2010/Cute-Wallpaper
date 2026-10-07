package com.cute.wallpaper.ringtones.data.media

import android.content.Context
import android.graphics.Movie
import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class MediaInfo(
    val durationMs: Long,
    val qualityLabel: String?
)

@Singleton
class MediaInfoResolver @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val cache = ConcurrentHashMap<String, MediaInfo>()
    private val mutex = Mutex()
    private val durationCache = ConcurrentHashMap<String, Long>()
    private val durationMutexes = ConcurrentHashMap<String, Mutex>()

    suspend fun resolve(url: String): MediaInfo? {
        if (url.isBlank()) return null
        cache[url]?.let { return it }

        if (url.isGif()) {
            val duration = resolveDuration(url) ?: return null
            return MediaInfo(durationMs = duration, qualityLabel = null).also {
                cache[url] = it
            }
        }

        return mutex.withLock {
            cache[url]?.let { return@withLock it }
            withTimeoutOrNull(RESOLVE_TIMEOUT_MS) {
                resolveWithExoPlayer(url)
            }?.also {
                cache[url] = it
                durationCache[url] = it.durationMs
            }
        }
    }

    suspend fun resolveDuration(url: String): Long? {
        if (url.isBlank()) return null
        cache[url]?.durationMs?.takeIf { it > 0 }?.let { return it }
        durationCache[url]?.takeIf { it > 0 }?.let { return it }

        val urlMutex = durationMutexes.getOrPut(url) { Mutex() }
        return urlMutex.withLock {
            cache[url]?.durationMs?.takeIf { it > 0 }?.let { return@withLock it }
            durationCache[url]?.takeIf { it > 0 }?.let { return@withLock it }

            withTimeoutOrNull(RESOLVE_TIMEOUT_MS) {
                if (url.isGif()) {
                    resolveGifDuration(url)
                } else {
                    resolveDurationWithExoPlayer(url)
                }
            }?.takeIf { it > 0 }?.also { duration ->
                durationCache[url] = duration
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun resolveGifDuration(url: String): Long? =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = CONNECT_TIMEOUT_MS
                    connection.readTimeout = READ_TIMEOUT_MS
                    connection.instanceFollowRedirects = true
                    connection.connect()
                    if (connection.responseCode !in 200..299) return@runCatching null

                    BufferedInputStream(connection.inputStream).use { input ->
                        Movie.decodeStream(input)
                            ?.duration()
                            ?.toLong()
                            ?.takeIf { it > 0L }
                    }
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }

    private fun String.isGif(): Boolean =
        substringBefore('?').substringBefore('#').endsWith(".gif", ignoreCase = true)

    private suspend fun resolveWithExoPlayer(url: String): MediaInfo? =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val player = ExoPlayer.Builder(context).build()
                var finished = false
                lateinit var listener: Player.Listener

                fun finish(result: MediaInfo?) {
                    if (finished) return
                    finished = true
                    player.removeListener(listener)
                    player.release()
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                fun finishIfMediaInfoReady() {
                    if (player.playbackState != Player.STATE_READY) return

                    val duration = player.duration
                        .takeIf { it > 0 && it != C.TIME_UNSET }
                        ?: return
                    val quality = player.videoFormat?.height
                        ?.takeIf { it > 0 }
                        ?.toQualityLabel()

                    finish(
                        MediaInfo(
                            durationMs = duration,
                            qualityLabel = quality
                        )
                    )
                }

                listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        finishIfMediaInfoReady()
                    }

                    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                        finishIfMediaInfoReady()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        finish(null)
                    }
                }

                continuation.invokeOnCancellation {
                    Handler(player.applicationLooper).post {
                        if (!finished) {
                            finished = true
                            player.removeListener(listener)
                            player.release()
                        }
                    }
                }

                player.addListener(listener)
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
            }
        }

    private suspend fun resolveDurationWithExoPlayer(url: String): Long? =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val player = ExoPlayer.Builder(context).build()
                var finished = false
                lateinit var listener: Player.Listener

                fun finish(result: Long?) {
                    if (finished) return
                    finished = true
                    player.removeListener(listener)
                    player.release()
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                fun finishIfDurationReady() {
                    val duration = player.duration
                        .takeIf { it > 0 && it != C.TIME_UNSET }
                        ?: return
                    finish(duration)
                }

                listener = object : Player.Listener {
                    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                        finishIfDurationReady()
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        finishIfDurationReady()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        finishIfDurationReady()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        finish(null)
                    }
                }

                continuation.invokeOnCancellation {
                    Handler(player.applicationLooper).post {
                        if (!finished) {
                            finished = true
                            player.removeListener(listener)
                            player.release()
                        }
                    }
                }

                player.addListener(listener)
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
                finishIfDurationReady()
            }
        }

    private fun Int.toQualityLabel(): String = when {
        this >= 2160 -> "4K"
        this >= 1440 -> "2K"
        this >= 1080 -> "FHD"
        this >= 720 -> "HD"
        else -> "${this}p"
    }

    private companion object {
        const val RESOLVE_TIMEOUT_MS = 30_000L
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 20_000
    }
}
