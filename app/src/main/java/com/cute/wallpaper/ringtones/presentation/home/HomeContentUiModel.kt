package com.cute.wallpaper.ringtones.presentation.home

import com.cute.wallpaper.ringtones.domain.model.ContentRef
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.domain.model.ContentItem

data class HomeContentUiModel(
    val id: String,
    val title: String,
    val type: ContentType,
    val category: String,
    val collection: WallpaperCollection?,
    val thumbnailUrl: String?,
    val contentUrl: String?,
    val secondaryContentUrl: String?,
    val quote: String?,
    val quoteCategoryId: Int?,
    val color: String?,
    val tags: Set<String>,
    val rank: Int,
    val downloadEnabled: Boolean
) {
    val ref: ContentRef get() = ContentRef(type, id)

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        return title.contains(query, ignoreCase = true) ||
            quote.orEmpty().contains(query, ignoreCase = true) ||
            category.contains(query, ignoreCase = true) ||
            tags.any { it.contains(query, ignoreCase = true) }
    }
}

fun ContentItem.toUiModel() = HomeContentUiModel(
    id = id,
    title = title,
    type = type,
    category = category,
    collection = WallpaperCollection.pages.firstOrNull { it.category == category },
    thumbnailUrl = thumbnailUrl,
    contentUrl = contentUrl,
    secondaryContentUrl = secondaryContentUrl,
    quote = quote,
    quoteCategoryId = quoteCategoryId,
    color = color,
    tags = tags,
    rank = rank,
    downloadEnabled = downloadEnabled
)
