@file:Suppress("DEPRECATION")

package com.cute.wallpaper.ringtones.service

import android.graphics.Color
import android.graphics.Movie
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.cute.wallpaper.ringtones.data.local.preference.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class LiveWallpaperService : WallpaperService() {
    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreateEngine(): Engine = GifEngine()

    inner class GifEngine : Engine() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val handler = Handler(Looper.getMainLooper())
        private val drawFrame = Runnable { drawMovieFrame() }

        private var movie: Movie? = null
        private var isVisible = false
        private var startTime = 0L

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            scope.launch {
                val url = appPreferences.liveWallpaperUrl.first().orEmpty()
                if (url.isBlank()) return@launch
                val file = cacheGif(url) ?: return@launch
                val decoded = Movie.decodeFile(file.absolutePath) ?: return@launch
                withContext(Dispatchers.Main) {
                    movie = decoded
                    startTime = 0L
                    if (isVisible) drawMovieFrame()
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) {
                drawMovieFrame()
            } else {
                handler.removeCallbacks(drawFrame)
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            if (isVisible) drawMovieFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            isVisible = false
            handler.removeCallbacks(drawFrame)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            handler.removeCallbacks(drawFrame)
            scope.cancel()
            movie = null
            super.onDestroy()
        }

        private fun drawMovieFrame() {
            handler.removeCallbacks(drawFrame)
            val gif = movie
            if (!isVisible || gif == null) return

            val canvas = runCatching { surfaceHolder.lockCanvas() }.getOrNull()
            if (canvas != null) {
                try {
                    canvas.drawColor(Color.BLACK)
                    val now = SystemClock.uptimeMillis()
                    if (startTime == 0L) startTime = now
                    val duration = gif.duration().takeIf { it > 0 } ?: DEFAULT_GIF_DURATION_MS
                    gif.setTime(((now - startTime) % duration).toInt())

                    val sourceWidth = gif.width().toFloat().coerceAtLeast(1f)
                    val sourceHeight = gif.height().toFloat().coerceAtLeast(1f)
                    val targetWidth = canvas.width.toFloat()
                    val targetHeight = canvas.height.toFloat()
                    val scale = maxOf(targetWidth / sourceWidth, targetHeight / sourceHeight)
                    val dx = (targetWidth - sourceWidth * scale) / 2f
                    val dy = (targetHeight - sourceHeight * scale) / 2f

                    canvas.save()
                    canvas.translate(dx, dy)
                    canvas.scale(scale, scale)
                    gif.draw(canvas, 0f, 0f)
                    canvas.restore()
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }

            if (isVisible) handler.postDelayed(drawFrame, FRAME_DELAY_MS)
        }

        private fun cacheGif(url: String): File? = runCatching {
            val directory = File(cacheDir, LIVE_WALLPAPER_CACHE_DIR).apply { mkdirs() }
            val file = File(directory, "${url.hashCode()}.gif")
            if (file.exists() && file.length() > 0L) return@runCatching file

            val tempFile = File(directory, "${url.hashCode()}.tmp")
            tempFile.delete()
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                connection.connect()
                check(connection.responseCode in 200..299)
                connection.inputStream.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                check(tempFile.length() > 0L)
                if (!tempFile.renameTo(file)) {
                    tempFile.copyTo(file, overwrite = true)
                    tempFile.delete()
                }
                directory.listFiles()
                    ?.filter { it != file }
                    ?.forEach(File::delete)
                file
            } finally {
                connection.disconnect()
                tempFile.delete()
            }
        }.getOrNull()
    }

    private companion object {
        const val LIVE_WALLPAPER_CACHE_DIR = "live_wallpaper"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val DEFAULT_GIF_DURATION_MS = 1_000
        const val FRAME_DELAY_MS = 16L
    }
}
