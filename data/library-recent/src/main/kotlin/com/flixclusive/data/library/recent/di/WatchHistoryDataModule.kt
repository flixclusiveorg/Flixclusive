package com.flixclusive.data.watch_history.di

import com.flixclusive.data.watch_history.DefaultWatchHistoryRepository
import com.flixclusive.data.watch_history.WatchHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WatchHistoryDataModule {

    @Singleton
    @Binds
    internal abstract fun bindsWatchHistoryRepository(
        watchHistoryRepository: DefaultWatchHistoryRepository,
    ): WatchHistoryRepository

}