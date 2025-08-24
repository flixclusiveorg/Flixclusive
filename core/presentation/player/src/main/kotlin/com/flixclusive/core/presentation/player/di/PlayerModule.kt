package com.flixclusive.core.presentation.player.di

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.presentation.player.InternalPlayer
import com.flixclusive.core.presentation.player.InternalPlayerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

@Module
@InstallIn(ViewModelComponent::class)
internal object PlayerModule {
    @Provides
    fun providerInternalPlayer(
        @ApplicationContext context: Context,
        client: OkHttpClient,
        dataStoreManager: DataStoreManager,
        appDispatchers: AppDispatchers
    ): InternalPlayer {
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

        return InternalPlayerImpl(
            client = client,
            context = context,
            subtitlePrefs = subtitlePrefs,
            playerPrefs = playerPrefs
        )
    }
}
