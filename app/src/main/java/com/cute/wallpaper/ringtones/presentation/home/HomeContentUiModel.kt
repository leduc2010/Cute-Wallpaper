package com.cute.wallpaper.ringtones.presentation.home

import com.cute.wallpaper.ringtones.core.model.ContentRef
import com.cute.wallpaper.ringtones.core.model.ContentType
import com.cute.wallpaper.ringtones.data.fake.FakeContentRecord
import com.cute.wallpaper.ringtones.presentation.home.demo.DemoCollection

data class HomeContentUiModel(
    val id: String,
    val title: String,
    val type: ContentType,
    val collection: DemoCollection,
    val artworkIndex: Int?,
    val quote: String?,
    val colorIds: Set<String>,
    val genreIds: Set<String>,
    val keywords: Set<String>
) {
    val ref: ContentRef get() = ContentRef(type, id)

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        return title.contains(query, ignoreCase = true) ||
            quote.orEmpty().contains(query, ignoreCase = true) ||
            keywords.any { it.contains(query, ignoreCase = true) }
    }
}

fun FakeContentRecord.toUiModel() = HomeContentUiModel(
    id = id,
    title = title,
    type = type,
    collection = DemoCollection.fromDataId(collectionId),
    artworkIndex = artworkIndex,
    quote = quote,
    colorIds = colorIds,
    genreIds = genreIds,
    keywords = keywords
)
