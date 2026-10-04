package com.cute.wallpaper.ringtones.di

import com.cute.wallpaper.ringtones.data.repository.WallpaperRepositoryImpl
import com.cute.wallpaper.ringtones.domain.repository.WallpaperRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WallpaperModule {

    @Binds
    @Singleton
    abstract fun bindWallpaperRepository(
        implementation: WallpaperRepositoryImpl
    ): WallpaperRepository
}
