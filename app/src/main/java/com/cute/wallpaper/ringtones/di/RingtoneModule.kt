package com.cute.wallpaper.ringtones.di

import com.cute.wallpaper.ringtones.data.repository.RingtoneRepositoryImpl
import com.cute.wallpaper.ringtones.domain.repository.RingtoneRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RingtoneModule {

    @Binds
    @Singleton
    abstract fun bindRingtoneRepository(
        implementation: RingtoneRepositoryImpl
    ): RingtoneRepository
}
