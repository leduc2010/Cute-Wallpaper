package com.cute.wallpaper.ringtones.domain.repository

import com.cute.wallpaper.ringtones.domain.model.RingtoneTarget

interface RingtoneRepository {
    fun canWriteSystemSettings(): Boolean
    fun hasLegacyStoragePermission(): Boolean

    suspend fun setSystemSound(
        url: String,
        title: String,
        fileName: String,
        target: RingtoneTarget
    ): Boolean
}
