package com.flixclusive.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.flixclusive.core.datastore.serializer.AppSettingsProviderSerializer
import com.flixclusive.core.datastore.serializer.AppSettingsSerializer
import com.flixclusive.core.datastore.serializer.OnBoardingPreferencesSerializer
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.AppSettingsProvider
import com.flixclusive.model.datastore.OnBoardingPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


val Context.appSettings by dataStore("app-preferences.json", AppSettingsSerializer)
val Context.appProviderSettings by dataStore("app-provider-preferences.json", AppSettingsProviderSerializer)
val Context.onBoardingPreferences by dataStore("on-boarding-preferences.json", OnBoardingPreferencesSerializer)

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    @Provides
    @Singleton
    fun providesUserPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<AppSettings> = context.appSettings

    @Provides
    @Singleton
    fun providesUserProviderPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<AppSettingsProvider> = context.appProviderSettings

    @Provides
    @Singleton
    fun providesOnBoardingPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<OnBoardingPreferences> = context.onBoardingPreferences
}