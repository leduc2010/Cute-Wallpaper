package com.cute.wallpaper.ringtones.presentation.favorites

import com.cute.wallpaper.ringtones.domain.model.ContentItem
import com.cute.wallpaper.ringtones.domain.model.ContentType
import com.cute.wallpaper.ringtones.presentation.home.toUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteCategoryTest {
    @Test fun dualWallpaperBelongsOnlyToDualFilter() {
        val item = ContentItem("dual-1", ContentType.WALLPAPER, "dual", "Dual").toUiModel()
        assertTrue(FavoriteCategory.DUAL.matches(item))
        assertFalse(FavoriteCategory.WALLPAPER.matches(item))
        assertEquals(listOf(FavoriteCategory.DUAL), FavoriteCategory.entries.filter { it.matches(item) })
    }

    @Test fun bestWallpapersRemainAccessibleInWallpaperFilter() {
        val item = ContentItem("best-1", ContentType.WALLPAPER, "best", "Best").toUiModel()
        assertTrue(FavoriteCategory.WALLPAPER.matches(item))
        assertFalse(FavoriteCategory.DUAL.matches(item))
    }

    @Test fun profileFavoritesRetainSeparateTypeEvenWhenIdsOverlap() {
        val profile = ContentItem("same", ContentType.PROFILE_PICTURE, "profile", "Profile").toUiModel()
        val wallpaper = ContentItem("same", ContentType.WALLPAPER, "wallpaper", "Wallpaper").toUiModel()
        assertEquals(listOf(FavoriteCategory.PROFILE), FavoriteCategory.entries.filter { it.matches(profile) })
        assertFalse(profile.ref.toFavoriteKey() == wallpaper.ref.toFavoriteKey())
    }
}
