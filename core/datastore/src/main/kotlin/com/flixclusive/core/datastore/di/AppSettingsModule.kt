package com.flixclusive.core.datastore.di

import androidx.datastore.core.DataStore
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.AppSettingsProvider
import com.flixclusive.model.datastore.OnBoardingPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppSettingsModule {
    @Provides
    @Singleton
    fun providesAppSettingsManager(
        appSettings: DataStore<AppSettings>,
        appProviderSettings: DataStore<AppSettingsProvider>,
        onBoardingPreferences: DataStore<OnBoardingPreferences>
    ): AppSettingsManager = AppSettingsManager(
        appSettings = appSettings,
        providerSettings = appProviderSettings,
        onBoardingPreferences = onBoardingPreferences
    )
}