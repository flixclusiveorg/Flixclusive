package com.flixclusive.core.ui.player.di

import android.content.Context
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.ui.player.FlixclusivePlayerManager
import com.flixclusive.core.ui.player.util.PlayerCacheManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import okhttp3.OkHttpClient

@Module
@InstallIn(ViewModelComponent::class)
internal object FlixclusivePlayerManagerModule {
    @Provides
    @ViewModelScoped
    fun providesFlixclusivePlayerManager(
        @ApplicationContext context: Context,
        playerCacheManager: PlayerCacheManager,
        appSettingsManager: AppSettingsManager,
        client: OkHttpClient
    ) = FlixclusivePlayerManager(
        context = context,
        playerCacheManager = playerCacheManager,
        appSettingsManager = appSettingsManager,
        client = client
    )
}
