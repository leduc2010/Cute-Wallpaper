package com.cute.wallpaper.ringtones.di

import com.cute.wallpaper.ringtones.ads.AdConfigImpl
import com.cute.wallpaper.ringtones.presentation.language.LanguageImpl
import com.cute.wallpaper.ringtones.presentation.onboarding.OnboardingImpl
import com.cute.wallpaper.ringtones.presentation.splash.SplashImpl
import com.cute.wallpaper.ringtones.presentation.uninstall.UninstallImpl
import com.cute.wallpaper.ringtones.presentation.welcome_back.WelcomeBackImpl
import com.leansoft.ads.AdConfig
import com.leansoft.ads.ui.language.LeansoftLanguageInterface
import com.leansoft.ads.ui.onboarding.LeansoftOnboardingInterface
import com.leansoft.ads.ui.splash.LeansoftSplashInterface
import com.leansoft.ads.ui.uninstall.LeansoftUninstallInterface
import com.leansoft.ads.ui.welcome_back.LeansoftWelcomeBackInterface
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdsModule {
    @Provides @Singleton
    fun provideAdConfig(impl: AdConfigImpl): AdConfig = impl

    @Provides @Singleton
    fun provideSplashInterface(impl: SplashImpl): LeansoftSplashInterface = impl

    @Provides @Singleton
    fun provideLanguageInterface(impl: LanguageImpl): LeansoftLanguageInterface = impl

    @Provides @Singleton
    fun provideOnboardingInterface(impl: OnboardingImpl): LeansoftOnboardingInterface = impl

    @Provides @Singleton
    fun provideUninstallInterface(impl: UninstallImpl): LeansoftUninstallInterface = impl

    @Provides @Singleton
    fun provideWelcomeBackInterface(impl: WelcomeBackImpl): LeansoftWelcomeBackInterface = impl
}
