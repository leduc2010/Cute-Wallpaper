package com.cute.wallpaper.ringtones.data.repository

import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.data.remote.RemoteContentDataSource
import com.cute.wallpaper.ringtones.data.remote.dto.QuoteDataDto
import com.cute.wallpaper.ringtones.domain.model.ContentItem
import com.cute.wallpaper.ringtones.domain.model.QuoteCategory
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val remoteContentDataSource: RemoteContentDataSource
) : ContentRepository {

    @Volatile
    private var cachedContents: List<ContentItem>? = null
    @Volatile
    private var cachedQuoteCategories: List<QuoteCategory>? = null

    override fun getContents(): List<ContentItem> = cachedContents ?: refresh()

    override fun getQuoteCategories(): List<QuoteCategory> {
        if (cachedContents == null) refresh()
        return cachedQuoteCategories.orEmpty()
    }

    @Synchronized
    override fun refresh(): List<ContentItem> {
        val quoteData = remoteContentDataSource.getQuoteData()
        val contents = buildList {
            addAll(remoteContentDataSource.getWallpapers().mapNotNull { dto ->
                if (dto.content.isBlank()) return@mapNotNull null

                val type = when (dto.category.lowercase()) {
                    CATEGORY_LIVE -> ContentType.VIDEO_WALLPAPER
                    CATEGORY_PROFILE -> ContentType.PROFILE_PICTURE
                    else -> ContentType.WALLPAPER
                }
                val tags = dto.tag?.tags.orEmpty()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .toCollection(linkedSetOf())

                ContentItem(
                    id = stableId(dto.category, dto.id, dto.rank),
                    type = type,
                    category = dto.category.lowercase(),
                    title = tags.firstOrNull()?.replaceFirstChar(Char::titlecase)
                        ?: dto.category.replaceFirstChar(Char::titlecase),
                    thumbnailUrl = dto.thumb.ifBlank { dto.content },
                    contentUrl = dto.content,
                    secondaryContentUrl = dto.content2?.takeIf(String::isNotBlank),
                    color = dto.color?.lowercase(),
                    tags = tags,
                    rank = dto.rank,
                    downloadEnabled = dto.download
                )
            })

            addAll(remoteContentDataSource.getSounds().mapNotNull { dto ->
                if (dto.link.isBlank()) return@mapNotNull null
                ContentItem(
                    id = stableId(dto.category, dto.id, dto.rank),
                    type = ContentType.RINGTONE,
                    category = dto.category.lowercase(),
                    title = dto.name.ifBlank { dto.category.replaceFirstChar(Char::titlecase) },
                    contentUrl = dto.link,
                    tags = dto.tag?.takeIf(String::isNotBlank)?.let(::setOf).orEmpty(),
                    rank = dto.rank
                )
            })

            addAll(quoteData.toContentItems())
        }.sortedWith(compareBy<ContentItem> { it.type.ordinal }.thenBy { it.rank })
        cachedQuoteCategories = quoteData.toCategories()
        cachedContents = contents
        return contents
    }

    private fun stableId(category: String, id: Int, rank: Int): String =
        "${category.lowercase()}:$id:$rank"

    private companion object {
        const val CATEGORY_LIVE = "live"
        const val CATEGORY_PROFILE = "profile"
    }
}

internal fun QuoteDataDto.toCategories(): List<QuoteCategory> = categories.mapNotNull { category ->
    val name = category.name.trim()
    if (name.isBlank() || name.equals("new", ignoreCase = true) || name.equals("popular", ignoreCase = true)) {
        return@mapNotNull null
    }
    QuoteCategory(
        id = category.id,
        name = name,
        thumbnailUrl = category.thumb.takeIf(String::isNotBlank)
    )
}

internal fun QuoteDataDto.toContentItems(): List<ContentItem> {
    val categoriesById = categories
        .filter { it.name.isNotBlank() }
        .associateBy { it.id }

    return quotes.mapNotNull { quote ->
        val category = categoriesById[quote.categoryId] ?: return@mapNotNull null
        if (quote.content.isBlank()) return@mapNotNull null

        val bucket = when (category.name.trim().lowercase()) {
            "new" -> "new"
            "popular" -> "popular"
            else -> "category"
        }
        ContentItem(
            id = "$bucket:${quote.id}:${quote.rank}",
            type = ContentType.QUOTE,
            category = bucket,
            title = category.name.trim(),
            thumbnailUrl = quote.thumb.takeIf(String::isNotBlank)
                ?: category.thumb.takeIf(String::isNotBlank),
            quote = quote.content,
            quoteCategoryId = category.id,
            tags = setOf(category.name.trim()),
            rank = quote.rank
        )
    }
}
