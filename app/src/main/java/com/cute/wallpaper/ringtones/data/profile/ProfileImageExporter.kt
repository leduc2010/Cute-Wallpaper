package com.cute.wallpaper.ringtones.data.profile

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class ProfileImageExporter @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /** Legacy callers must obtain WRITE_EXTERNAL_STORAGE before invoking this on API 24–28. */
    suspend fun save(bitmap: Bitmap, filePrefix: String = "profile"): Uri? = withContext(Dispatchers.IO) {
        try {
            currentCoroutineContext().ensureActive()
            require(filePrefix.matches(Regex("[a-z_]+")))
            val name = "${filePrefix}_${UUID.randomUUID()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveScoped(bitmap, name)
            else saveLegacy(bitmap, name)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveScoped(bitmap: Bitmap, name: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Cute Wallpaper")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        var published = false
        try {
            currentCoroutineContext().ensureActive()
            requireNotNull(resolver.openOutputStream(uri)).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            currentCoroutineContext().ensureActive()
            val ready = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            check(resolver.update(uri, ready, null, null) > 0)
            published = true
            return uri
        } finally {
            if (!published) withContext(NonCancellable) {
                try {
                    resolver.delete(uri, null, null)
                } catch (_: Exception) {
                    // Preserve the original write/cancellation failure if the provider rejects cleanup.
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun saveLegacy(bitmap: Bitmap, name: String): Uri {
        val folder = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "Cute Wallpaper")
        check(folder.isDirectory || folder.mkdirs())
        val file = File(folder, name)
        var complete = false
        try {
            currentCoroutineContext().ensureActive()
            file.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            currentCoroutineContext().ensureActive()
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(MIME_TYPE), null)
            complete = true
            return Uri.fromFile(file)
        } finally {
            if (!complete) file.delete()
        }
    }

    private companion object {
        const val MIME_TYPE = "image/png"
    }
}
