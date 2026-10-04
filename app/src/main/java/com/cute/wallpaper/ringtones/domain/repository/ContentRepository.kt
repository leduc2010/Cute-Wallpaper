package com.cute.wallpaper.ringtones.domain.repository

import com.cute.wallpaper.ringtones.domain.model.ContentItem

interface ContentRepository {
    fun getContents(): List<ContentItem>
    fun refresh(): List<ContentItem>
}
