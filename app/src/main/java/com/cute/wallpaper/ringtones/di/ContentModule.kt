package com.cute.wallpaper.ringtones.di

import com.cute.wallpaper.ringtones.data.repository.ContentRepositoryImpl
import com.cute.wallpaper.ringtones.domain.repository.ContentRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ContentModule {

    @Binds
    @Singleton
    abstract fun bindContentRepository(
        implementation: ContentRepositoryImpl
    ): ContentRepository

    companion object {
        @Provides
        @Singleton
        fun provideGson(): Gson = GsonBuilder().create()
    }
}
