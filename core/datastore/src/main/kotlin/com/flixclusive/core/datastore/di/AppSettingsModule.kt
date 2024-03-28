package com.flixclusive.core.datastore.di

import androidx.datastore.core.DataStore
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.AppSettingsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppSettingsModule {
    @Provides
    @Singleton
    internal fun providesAppSettingsManager(
        appSettings: DataStore<AppSettings>,
        appProviderSettings: DataStore<AppSettingsProvider>,
        @ApplicationScope scope: CoroutineScope
    ): AppSettingsManager = AppSettingsManager(
        appSettings = appSettings,
        providerSettings = appProviderSettings,
        scope = scope
    )
}