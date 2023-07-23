package com.flixclusive.di

import com.flixclusive.common.Constants.CONSUMET_API_BASE_HOST
import com.flixclusive.data.api.utils.HostSelectionInterceptor
import com.flixclusive.data.firebase.ConfigurationProviderImpl
import com.flixclusive.domain.firebase.CONSUMET_API_HOST
import com.flixclusive.domain.firebase.CONSUMET_DEFAULT_VIDEO_SERVER
import com.flixclusive.domain.firebase.CONSUMET_DEFAULT_WATCH_PROVIDER
import com.flixclusive.domain.firebase.ConfigurationProvider
import com.flixclusive.domain.firebase.FLIXCLUSIVE_IS_MAINTENANCE
import com.flixclusive.domain.firebase.FLIXCLUSIVE_LATEST_VERSION
import com.flixclusive.domain.firebase.FLIXCLUSIVE_UPDATE_URL
import com.flixclusive.domain.firebase.TMDB_API_KEY
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigModule {
    @Provides
    @Singleton
    fun providesFirebaseRemoteConfig(): FirebaseRemoteConfig = Firebase.remoteConfig
        .apply {
            val defaultConsumetHost = CONSUMET_API_BASE_HOST
            val defaultConsumetVideoServer = "upcloud"
            val defaultConsumetWatchProvider = "flixhq"
            val defaultTMDBApiKey = ""
            val defaultUpdatePage = "https://github.com/rhenwinch/Flixclusive/releases/latest"

            val defaults = mutableMapOf<String, Any>(
                CONSUMET_API_HOST to defaultConsumetHost,
                CONSUMET_DEFAULT_WATCH_PROVIDER to defaultConsumetWatchProvider,
                TMDB_API_KEY to defaultTMDBApiKey,
                CONSUMET_DEFAULT_VIDEO_SERVER to defaultConsumetVideoServer,
                FLIXCLUSIVE_LATEST_VERSION to -1L,
                FLIXCLUSIVE_UPDATE_URL to defaultUpdatePage,
                FLIXCLUSIVE_IS_MAINTENANCE to false,
            )

            setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = 3600
                }
            )
            setDefaultsAsync(defaults)
        }

    @Provides
    @Singleton
    fun providesConfigurationProvider(
        remoteConfig: FirebaseRemoteConfig,
        hostSelectionInterceptor: HostSelectionInterceptor
    ): ConfigurationProvider = ConfigurationProviderImpl(
        remoteConfig = remoteConfig,
        hostSelectionInterceptor = hostSelectionInterceptor
    )
}