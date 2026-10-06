package com.cute.wallpaper.ringtones.background.autochangewallpaper

import com.cute.wallpaper.ringtones.domain.model.DownloadedWallpaper
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoChangeWallpaperWorkerTest {

    private val wallpapers = listOf(
        DownloadedWallpaper("one", "file:///one.jpg"),
        DownloadedWallpaper("two", "file:///two.jpg"),
        DownloadedWallpaper("three", "file:///three.jpg")
    )

    @Test
    fun noPreviousWallpaper_startsFromFirst() {
        assertEquals(
            listOf("one", "two", "three"),
            orderAutoChangeCandidates(wallpapers, null).map { it.key }
        )
    }

    @Test
    fun previousWallpaper_rotatesFromNextItem() {
        assertEquals(
            listOf("three", "one", "two"),
            orderAutoChangeCandidates(wallpapers, "two").map { it.key }
        )
    }

    @Test
    fun missingPreviousWallpaper_fallsBackToFirst() {
        assertEquals(
            listOf("one", "two", "three"),
            orderAutoChangeCandidates(wallpapers, "deleted").map { it.key }
        )
    }
}
