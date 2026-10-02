package com.cute.wallpaper.ringtones.presentation.home.demo

import com.cute.wallpaper.ringtones.data.fake.FakeCollectionIds

enum class DemoCollection(val dataId: String) {
    WALLPAPER(FakeCollectionIds.WALLPAPER),
    DUAL_WALLPAPERS(FakeCollectionIds.DUAL_WALLPAPERS),
    BEST(FakeCollectionIds.BEST),

    @Deprecated("Saved-state compatibility only")
    ALL(FakeCollectionIds.WALLPAPER),

    @Deprecated("Saved-state compatibility only")
    FANTASY(FakeCollectionIds.DUAL_WALLPAPERS),

    @Deprecated("Saved-state compatibility only")
    BUTTERFLY(FakeCollectionIds.BEST);

    fun canonical(): DemoCollection = when (this) {
        WALLPAPER, ALL -> WALLPAPER
        DUAL_WALLPAPERS, FANTASY -> DUAL_WALLPAPERS
        BEST, BUTTERFLY -> BEST
    }

    companion object {
        val pages = listOf(WALLPAPER, DUAL_WALLPAPERS, BEST)

        fun fromDataId(value: String): DemoCollection {
            return pages.firstOrNull { it.dataId == value } ?: WALLPAPER
        }
    }
}
