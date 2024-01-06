package com.flixclusive.data.configuration.di

import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.configuration.DefaultAppConfigurationManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppConfigurationModule {
    @Binds
    internal abstract fun bindsAppConfigurationProvider(
        appConfigurationProvider: DefaultAppConfigurationManager,
    ): AppConfigurationManager
}