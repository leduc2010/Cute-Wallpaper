package com.cute.wallpaper.ringtones.data.fake

import com.cute.wallpaper.ringtones.core.model.ContentType
import javax.inject.Inject
import javax.inject.Singleton

data class FakeArtworkRegion(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class FakeContentRecord(
    val id: String,
    val title: String,
    val type: ContentType,
    val collectionId: String = FakeCollectionIds.WALLPAPER,
    val artworkIndex: Int? = null,
    val quote: String? = null,
    val colorIds: Set<String> = emptySet(),
    val genreIds: Set<String> = emptySet(),
    val keywords: Set<String> = emptySet()
)

data class FakeColorRecord(
    val id: String,
    val label: String,
    val colorHex: String
)

data class FakeGenreRecord(
    val id: String,
    val label: String,
    val emoji: String
)

enum class FakeQuickFilterKind { COLOR, GENRE }

data class FakeQuickFilterRecord(
    val id: String,
    val label: String,
    val emoji: String,
    val kind: FakeQuickFilterKind
)

object FakeCollectionIds {
    const val WALLPAPER = "wallpaper"
    const val DUAL_WALLPAPERS = "dual_wallpapers"
    const val BEST = "best"

    val all = listOf(WALLPAPER, DUAL_WALLPAPERS, BEST)
}

/** Temporary catalog used until the remote contract is available. */
@Singleton
class FakeContentDataSource @Inject constructor() {
    val artworkRegions = listOf(
        FakeArtworkRegion(32, 231, 200, 525),
        FakeArtworkRegion(215, 231, 383, 525),
        FakeArtworkRegion(32, 541, 200, 798),
        FakeArtworkRegion(215, 541, 383, 798)
    )

    val colors = listOf(
        FakeColorRecord("pink", "Pink", "#FF6384"),
        FakeColorRecord("green", "Green", "#46B050"),
        FakeColorRecord("blue", "Blue", "#2196ED"),
        FakeColorRecord("black", "Black", "#000000"),
        FakeColorRecord("purple", "Purple", "#9C27B0"),
        FakeColorRecord("white", "White", "#FFFFFF"),
        FakeColorRecord("red", "Red", "#F64032"),
        FakeColorRecord("orange", "Orange", "#FF9000"),
        FakeColorRecord("cyan", "Cyan", "#08B5C7"),
        FakeColorRecord("yellow", "Yellow", "#FFE13B")
    )

    val genres = listOf(
        FakeGenreRecord("hoodie", "hoodie", "👚"),
        FakeGenreRecord("love", "love", "💗"),
        FakeGenreRecord("sparkles", "sparkles", "✨"),
        FakeGenreRecord("ocean", "ocean", "🌊"),
        FakeGenreRecord("winter", "winter", "❄️"),
        FakeGenreRecord("black", "black", "🖤"),
        FakeGenreRecord("bird", "bird", "🐦"),
        FakeGenreRecord("flowers", "flowers", "🌸"),
        FakeGenreRecord("new", "new", "✨"),
        FakeGenreRecord("halloween", "halloween", "🎃"),
        FakeGenreRecord("hot", "hot", "🔥")
    )

    val quickFilters = listOf(
        FakeQuickFilterRecord("pink", "Pink", "💗", FakeQuickFilterKind.COLOR),
        FakeQuickFilterRecord("new", "New", "✨", FakeQuickFilterKind.GENRE),
        FakeQuickFilterRecord("halloween", "Halloween", "🎃", FakeQuickFilterKind.GENRE),
        FakeQuickFilterRecord("hot", "Hot", "🔥", FakeQuickFilterKind.GENRE),
        FakeQuickFilterRecord("love", "Love", "💗", FakeQuickFilterKind.GENRE)
    )

    val contents: List<FakeContentRecord> = buildList {
        val artworkTitles = listOf(
            "Moonlight fairy",
            "Starlit dream",
            "Cosmic butterfly",
            "Blue princess",
            "Pink moon castle",
            "Lavender dream",
            "Fairy sky",
            "Royal garden",
            "Celestial whale",
            "Magic galaxy",
            "Winter princess",
            "Flower moon",
            "Cloud kingdom",
            "Aurora angel",
            "Crystal lake",
            "Dreamy portrait",
            "Midnight roses",
            "Pastel stars"
        )
        val colorIds = colors.map(FakeColorRecord::id)
        val genreIds = genres.map(FakeGenreRecord::id)

        listOf(ContentType.WALLPAPER, ContentType.VIDEO_WALLPAPER, ContentType.PROFILE_PICTURE)
            .forEach { type ->
                FakeCollectionIds.all.forEachIndexed { collectionIndex, collectionId ->
                    artworkTitles.forEachIndexed { index, title ->
                        val id = if (collectionId == FakeCollectionIds.WALLPAPER) {
                            "demo_${type.name.lowercase()}_$index"
                        } else {
                            "demo_${type.name.lowercase()}_${collectionId}_$index"
                        }
                        val seed = index + collectionIndex * 3
                        add(
                            FakeContentRecord(
                                id = id,
                                title = title,
                                type = type,
                                collectionId = collectionId,
                                artworkIndex = index % artworkRegions.size,
                                colorIds = colorIds.filterIndexed { colorIndex, _ ->
                                    (seed + colorIndex) % 2 == 0
                                }.toSet(),
                                genreIds = genreIds.filterIndexed { genreIndex, _ ->
                                    (seed + genreIndex) % 3 != 0
                                }.toSet(),
                                keywords = setOf("cute", "wallpaper", title.lowercase())
                            )
                        )
                    }
                }
            }

        listOf("Soft chimes", "Fairy bells", "Moonlight melody", "Sweet morning")
            .forEachIndexed { index, title ->
                add(
                    FakeContentRecord(
                        id = "demo_ringtone_$index",
                        title = title,
                        type = ContentType.RINGTONE,
                        genreIds = setOf(genreIds[index % genreIds.size]),
                        keywords = setOf("cute", "sound", title.lowercase())
                    )
                )
            }

        listOf(
            "A little magic" to "Leave a little sparkle wherever you go.",
            "Dream softly" to "Let your dreams bloom.",
            "Your own light" to "Shine in your own beautiful way.",
            "Small joys" to "Find magic in the little things."
        ).forEachIndexed { index, (title, quote) ->
            add(
                FakeContentRecord(
                    id = "demo_quote_$index",
                    title = title,
                    type = ContentType.QUOTE,
                    quote = quote,
                    genreIds = setOf(genreIds[(index + 2) % genreIds.size]),
                    keywords = setOf("cute", "quote", title.lowercase(), quote.lowercase())
                )
            )
        }
    }
}
