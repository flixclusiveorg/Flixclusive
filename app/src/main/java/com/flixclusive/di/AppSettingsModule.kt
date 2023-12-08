package com.flixclusive.di

import android.content.Context
import androidx.datastore.core.DataStore
import com.flixclusive.appSettingsDataStore
import com.flixclusive.data.api.GithubConfigService
import com.flixclusive.data.config.ConfigurationProviderImpl
import com.flixclusive.data.preferences.AppSettingsManagerImpl
import com.flixclusive.domain.config.ConfigurationProvider
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.repository.ProvidersRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppSettingsModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<AppSettings> =
        context.appSettingsDataStore


    @Provides
    @Singleton
    fun providesAppSettingsManagerProvider(
        appSettings: DataStore<AppSettings>,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): AppSettingsManager = AppSettingsManagerImpl(
        appSettings = appSettings,
        ioScope = CoroutineScope(ioDispatcher)
    )

    @Provides
    @Singleton
    fun providesConfigurationProvider(
        githubConfigService: GithubConfigService,
        providersRepository: ProvidersRepository,
        appSettingsManager: AppSettingsManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ConfigurationProvider = ConfigurationProviderImpl(
        githubConfigService = githubConfigService,
        providersRepository = providersRepository,
        appSettingsManager = appSettingsManager,
        ioScope = CoroutineScope(ioDispatcher)
    )
}