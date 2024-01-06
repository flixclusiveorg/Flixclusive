package com.flixclusive.core.datastore.di

import com.flixclusive.core.datastore.AppSettingsManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppSettingsModule {
    @Binds
    internal abstract fun providesAppSettingsManager(): AppSettingsManager
}