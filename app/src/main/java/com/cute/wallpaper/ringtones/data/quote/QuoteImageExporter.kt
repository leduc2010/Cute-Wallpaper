package com.cute.wallpaper.ringtones.data.quote

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.cute.wallpaper.ringtones.data.profile.ProfileImageExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class QuoteImageExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageExporter: ProfileImageExporter
) {
    suspend fun save(bitmap: Bitmap): Uri? = imageExporter.save(bitmap, "quote")

    suspend fun share(bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, "shared_wallpapers")
        check(folder.isDirectory || folder.mkdirs())
        val file = File(folder, "quote_${UUID.randomUUID()}.png")
        var ready = false
        try {
            currentCoroutineContext().ensureActive()
            file.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            currentCoroutineContext().ensureActive()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            ready = true
            uri
        } finally {
            if (!ready) withContext(NonCancellable) { file.delete() }
        }
    }
}
