package com.cute.wallpaper.ringtones.data.repository

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.cute.wallpaper.ringtones.domain.model.RingtoneTarget
import com.cute.wallpaper.ringtones.domain.repository.RingtoneRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingtoneRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : RingtoneRepository {

    override fun canWriteSystemSettings(): Boolean = Settings.System.canWrite(context)

    override fun hasLegacyStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.P ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun setSystemSound(
        url: String,
        title: String,
        fileName: String,
        target: RingtoneTarget
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            resolver.delete(
                collection,
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                arrayOf(fileName)
            )

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.TITLE, title)
                put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE_MP3)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
                put(MediaStore.Audio.Media.IS_RINGTONE, target == RingtoneTarget.RINGTONE)
                put(
                    MediaStore.Audio.Media.IS_NOTIFICATION,
                    target == RingtoneTarget.NOTIFICATION
                )
                put(MediaStore.Audio.Media.IS_ALARM, target == RingtoneTarget.ALARM)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath(target))
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = requireNotNull(resolver.insert(collection, values))
            try {
                resolver.openOutputStream(uri)?.use { output ->
                    URL(url).openStream().use { input -> input.copyTo(output) }
                } ?: error("Cannot open audio output stream")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.update(
                        uri,
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        },
                        null,
                        null
                    )
                }

                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    ringtoneManagerType(target),
                    uri
                )
            } catch (throwable: Throwable) {
                resolver.delete(uri, null, null)
                throw throwable
            }
            true
        }.getOrDefault(false)
    }

    private fun ringtoneManagerType(target: RingtoneTarget): Int = when (target) {
        RingtoneTarget.RINGTONE -> RingtoneManager.TYPE_RINGTONE
        RingtoneTarget.NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
        RingtoneTarget.ALARM -> RingtoneManager.TYPE_ALARM
    }

    private fun relativePath(target: RingtoneTarget): String = when (target) {
        RingtoneTarget.RINGTONE -> "${Environment.DIRECTORY_RINGTONES}/Cute Wallpaper"
        RingtoneTarget.NOTIFICATION -> "${Environment.DIRECTORY_NOTIFICATIONS}/Cute Wallpaper"
        RingtoneTarget.ALARM -> "${Environment.DIRECTORY_ALARMS}/Cute Wallpaper"
    }

    private companion object {
        const val MIME_TYPE_MP3 = "audio/mpeg"
    }
}
