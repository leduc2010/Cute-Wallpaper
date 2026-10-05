package com.cute.wallpaper.ringtones.data.repository

import com.cute.wallpaper.ringtones.data.remote.dto.QuoteCategoryDto
import com.cute.wallpaper.ringtones.data.remote.dto.QuoteDataDto
import com.cute.wallpaper.ringtones.data.remote.dto.QuoteDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuoteDataMappingTest {
    @Test
    fun toContentItems_mapsTabsCategoriesRanksAndThumbnails() {
        val items = QuoteDataDto(
            categories = listOf(
                QuoteCategoryDto(0, "New", "new-category.webp"),
                QuoteCategoryDto(1, "Popular", "popular-category.webp"),
                QuoteCategoryDto(2, "Motivation", "motivation-category.webp")
            ),
            quotes = listOf(
                QuoteDto(10, 0, "New quote", 2, "new-quote.webp"),
                QuoteDto(11, 1, "Popular quote", 1, ""),
                QuoteDto(12, 2, "Motivation quote", 3, "motivation-quote.webp")
            )
        ).toContentItems()

        assertEquals(listOf("new", "popular", "category"), items.map { it.category })
        assertEquals(listOf(2, 1, 3), items.map { it.rank })
        assertEquals("new-quote.webp", items[0].thumbnailUrl)
        assertEquals("popular-category.webp", items[1].thumbnailUrl)
        assertEquals(2, items[2].quoteCategoryId)
        assertEquals(setOf("Motivation"), items[2].tags)
    }

    @Test
    fun toContentItems_skipsBlankQuotesAndUnknownCategories() {
        val items = QuoteDataDto(
            categories = listOf(QuoteCategoryDto(3, "Life", "")),
            quotes = listOf(
                QuoteDto(1, 3, "", 1, ""),
                QuoteDto(2, 99, "Orphan", 2, "orphan.webp"),
                QuoteDto(3, 3, "Valid", 3, "")
            )
        ).toContentItems()

        assertEquals(1, items.size)
        assertEquals("Valid", items.single().quote)
        assertNull(items.single().thumbnailUrl)
    }

    @Test
    fun toCategories_keepsRemoteOrderAndCategoriesWithoutQuotes() {
        val categories = QuoteDataDto(
            categories = listOf(
                QuoteCategoryDto(8, "Life", "life.webp"),
                QuoteCategoryDto(2, "Motivation", "motivation.webp"),
                QuoteCategoryDto(0, "New", "new.webp"),
                QuoteCategoryDto(1, "Popular", "popular.webp")
            ),
            quotes = emptyList()
        ).toCategories()

        assertEquals(listOf(8, 2), categories.map { it.id })
        assertEquals(listOf("Life", "Motivation"), categories.map { it.name })
        assertEquals("life.webp", categories.first().thumbnailUrl)
    }
}
