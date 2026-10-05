package com.cute.wallpaper.ringtones.domain.repository

import com.cute.wallpaper.ringtones.domain.model.ContentItem
import com.cute.wallpaper.ringtones.domain.model.QuoteCategory

interface ContentRepository {
    fun getContents(): List<ContentItem>
    fun getQuoteCategories(): List<QuoteCategory>
    fun refresh(): List<ContentItem>
}
