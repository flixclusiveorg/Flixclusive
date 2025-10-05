package com.flixclusive.core.presentation.player.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.presentation.player.AppDataSourceFactoryImpl
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.AppPlayerImpl
import com.flixclusive.core.presentation.player.PlayerCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
internal object PlayerModule {
    @Provides
    @Singleton
    fun providerAppPlayer(
        @ApplicationContext context: Context,
        dataStoreManager: DataStoreManager,
        appDispatchers: AppDispatchers,
        dataSourceFactory: AppDataSourceFactoryImpl
    ): AppPlayer {
        val subtitlePrefs = runBlocking(appDispatchers.io) {
            dataStoreManager.getUserPrefs(
                key = UserPreferences.SUBTITLES_PREFS_KEY,
                type = SubtitlesPreferences::class
            ).first()
        }

        val playerPrefs = runBlocking(appDispatchers.io) {
            dataStoreManager.getUserPrefs(
                key = UserPreferences.PLAYER_PREFS_KEY,
                type = PlayerPreferences::class
            ).first()
        }

        return AppPlayerImpl(
            context = context,
            subtitlePrefs = subtitlePrefs,
            playerPrefs = playerPrefs,
            dataSourceFactory = dataSourceFactory
        )
    }

    @Provides
    @Singleton
    fun providePlayerCache(
        @ApplicationContext context: Context
    ) = PlayerCache(context = context)

    @Provides
    @Singleton
    fun provideAppDataSourceFactory(
        @ApplicationContext context: Context,
        client: OkHttpClient,
        playerCache: PlayerCache,
        dataStoreManager: DataStoreManager,
        appDispatchers: AppDispatchers
    ): AppDataSourceFactoryImpl {
        val playerPreferences = runBlocking(appDispatchers.io) {
            dataStoreManager.getUserPrefs(
                key = UserPreferences.PLAYER_PREFS_KEY,
                type = PlayerPreferences::class
            ).first()
        }

        val preferredCacheSize = playerPreferences.diskCacheSize
        val cache = playerCache.get(preferredCacheSize)

        return AppDataSourceFactoryImpl(
            context = context,
            client = client,
            cache = cache
        )
    }
}
