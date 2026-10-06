package com.cute.wallpaper.ringtones.di

import com.cute.wallpaper.ringtones.data.local.autochangewallpaper.DownloadedWallpaperStoreImpl
import com.cute.wallpaper.ringtones.domain.repository.DownloadedWallpaperStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AutoChangeWallpaperModule {

    @Binds
    @Singleton
    abstract fun bindDownloadedWallpaperStore(
        implementation: DownloadedWallpaperStoreImpl
    ): DownloadedWallpaperStore
}
