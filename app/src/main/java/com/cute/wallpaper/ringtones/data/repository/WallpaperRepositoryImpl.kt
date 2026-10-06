package com.cute.wallpaper.ringtones.data.repository

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.cute.wallpaper.ringtones.domain.model.WallpaperTarget
import com.cute.wallpaper.ringtones.domain.repository.WallpaperRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : WallpaperRepository {

    override suspend fun setWallpaper(
        lockUrl: String,
        homeUrl: String?,
        target: WallpaperTarget
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val wallpaperManager = WallpaperManager.getInstance(context)
            when (target) {
                WallpaperTarget.HOME -> setWallpaper(
                    wallpaperManager,
                    homeUrl ?: lockUrl,
                    WallpaperManager.FLAG_SYSTEM
                )

                WallpaperTarget.LOCK -> setWallpaper(
                    wallpaperManager,
                    lockUrl,
                    WallpaperManager.FLAG_LOCK
                )

                WallpaperTarget.BOTH -> {
                    setWallpaper(
                        wallpaperManager,
                        homeUrl ?: lockUrl,
                        WallpaperManager.FLAG_SYSTEM
                    )
                    setWallpaper(
                        wallpaperManager,
                        lockUrl,
                        WallpaperManager.FLAG_LOCK
                    )
                }
            }
            true
        }.getOrDefault(false)
    }

    override suspend fun downloadWallpaper(url: String, fileName: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToMediaStore(url, fileName)
                } else {
                    saveLegacy(url, fileName)
                }
            }.getOrNull()
        }

    override suspend fun setWallpaperFromUri(
        uri: String,
        target: WallpaperTarget
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val wallpaperManager = WallpaperManager.getInstance(context)
            when (target) {
                WallpaperTarget.HOME -> setWallpaperFromUri(
                    wallpaperManager,
                    uri,
                    WallpaperManager.FLAG_SYSTEM
                )

                WallpaperTarget.LOCK -> setWallpaperFromUri(
                    wallpaperManager,
                    uri,
                    WallpaperManager.FLAG_LOCK
                )

                WallpaperTarget.BOTH -> {
                    setWallpaperFromUri(
                        wallpaperManager,
                        uri,
                        WallpaperManager.FLAG_SYSTEM
                    )
                    setWallpaperFromUri(
                        wallpaperManager,
                        uri,
                        WallpaperManager.FLAG_LOCK
                    )
                }
            }
            true
        }.getOrDefault(false)
    }

    override suspend fun prepareShareWallpaper(url: String, fileName: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val shareDirectory = File(context.cacheDir, SHARE_DIRECTORY).apply { mkdirs() }
                val shareFile = File(shareDirectory, fileName)
                URL(url).openStream().use { input ->
                    shareFile.outputStream().use { output -> input.copyTo(output) }
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    shareFile
                ).toString()
            }.getOrNull()
        }

    private fun setWallpaper(
        wallpaperManager: WallpaperManager,
        url: String,
        flag: Int
    ) {
        URL(url).openStream().use { input ->
            wallpaperManager.setStream(input, null, true, flag)
        }
    }

    private fun setWallpaperFromUri(
        wallpaperManager: WallpaperManager,
        uri: String,
        flag: Int
    ) {
        openDownloadedWallpaper(uri).use { input ->
            wallpaperManager.setStream(input, null, true, flag)
        }
    }

    private fun openDownloadedWallpaper(uri: String): InputStream {
        val parsed = Uri.parse(uri)
        return when (parsed.scheme) {
            "content" -> requireNotNull(context.contentResolver.openInputStream(parsed))
            "file" -> File(requireNotNull(parsed.path)).inputStream()
            "http", "https" -> URL(uri).openStream()
            else -> File(uri).inputStream()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(url: String, fileName: String): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType(url))
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/Cute Wallpaper"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        )

        try {
            resolver.openOutputStream(uri)?.use { output ->
                URL(url).openStream().use { input -> input.copyTo(output) }
            } ?: error("Cannot open MediaStore output stream")

            val ready = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, ready, null, null)
            return uri.toString()
        } catch (throwable: Throwable) {
            resolver.delete(uri, null, null)
            throw throwable
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(url: String, fileName: String): String {
        val pictures = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val folder = File(pictures, "Cute Wallpaper").apply { mkdirs() }
        val file = File(folder, fileName)
        URL(url).openStream().use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mimeType(url)),
            null
        )
        return Uri.fromFile(file).toString()
    }

    private fun mimeType(url: String): String {
        return URLConnection.guessContentTypeFromName(url.substringBefore('?'))
            ?: "image/jpeg"
    }

    private companion object {
        const val SHARE_DIRECTORY = "shared_wallpapers"
    }
}
