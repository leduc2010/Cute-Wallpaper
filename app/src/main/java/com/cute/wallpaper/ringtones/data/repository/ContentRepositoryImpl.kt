package com.cute.wallpaper.ringtones.data.repository

import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.data.remote.RemoteContentDataSource
import com.cute.wallpaper.ringtones.domain.model.ContentItem
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val remoteContentDataSource: RemoteContentDataSource
) : ContentRepository {

    @Volatile
    private var cachedContents: List<ContentItem>? = null

    override fun getContents(): List<ContentItem> = cachedContents ?: refresh()

    @Synchronized
    override fun refresh(): List<ContentItem> {
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

            addAll(remoteContentDataSource.getQuotes().mapNotNull { dto ->
                if (dto.content.isBlank()) return@mapNotNull null
                ContentItem(
                    id = stableId(dto.category, dto.id, dto.rank),
                    type = ContentType.QUOTE,
                    category = dto.category.lowercase(),
                    title = dto.tag?.takeIf(String::isNotBlank)
                        ?.replaceFirstChar(Char::titlecase)
                        ?: dto.category.replaceFirstChar(Char::titlecase),
                    quote = dto.content,
                    tags = dto.tag?.takeIf(String::isNotBlank)?.let(::setOf).orEmpty(),
                    rank = dto.rank
                )
            })
        }.sortedWith(compareBy<ContentItem> { it.type.ordinal }.thenBy { it.rank })
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
