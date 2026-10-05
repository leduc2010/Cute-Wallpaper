package com.cute.wallpaper.ringtones.data.remote

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteContentDataSourceTest {
    private val source = RemoteContentDataSource(Gson())

    @Test
    fun parseQuoteData_readsCategoryReferencesAndThumbnails() {
        val data = source.parseQuoteData(
            """
            {
              "categories": [{"id": 2, "name": "Motivation", "thumb": "category.webp"}],
              "quotes": [{"id": 7, "category_id": 2, "content": "Keep going.", "rank": 1, "thumb": "quote.webp"}]
            }
            """.trimIndent()
        )

        assertEquals("Motivation", data.categories.single().name)
        assertEquals(2, data.quotes.single().categoryId)
        assertEquals("quote.webp", data.quotes.single().thumb)
    }

    @Test
    fun parseQuoteData_returnsEmptyDataForInvalidPayload() {
        val data = source.parseQuoteData("[]")

        assertTrue(data.categories.isEmpty())
        assertTrue(data.quotes.isEmpty())
    }

    @Test
    fun parseQuoteData_normalizesExplicitNullCollectionsAndValues() {
        val nullCollections = source.parseQuoteData("""{"categories":null,"quotes":null}""")
        val nullValues = source.parseQuoteData(
            """{"categories":[null,{"id":2,"name":null,"thumb":null}],"quotes":[]}"""
        )

        assertTrue(nullCollections.categories.isEmpty())
        assertTrue(nullCollections.quotes.isEmpty())
        assertEquals("", nullValues.categories.single().name)
        assertEquals("", nullValues.categories.single().thumb)
    }
}
