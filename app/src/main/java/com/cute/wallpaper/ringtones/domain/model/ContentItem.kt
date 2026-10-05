package com.cute.wallpaper.ringtones.domain.model

data class ContentItem(
    val id: String,
    val type: ContentType,
    val category: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val contentUrl: String? = null,
    val secondaryContentUrl: String? = null,
    val quote: String? = null,
    val quoteCategoryId: Int? = null,
    val color: String? = null,
    val tags: Set<String> = emptySet(),
    val rank: Int = 0,
    val downloadEnabled: Boolean = false
)
