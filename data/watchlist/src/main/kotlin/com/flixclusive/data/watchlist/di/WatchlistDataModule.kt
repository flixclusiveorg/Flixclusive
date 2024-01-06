package com.flixclusive.data.watchlist.di

import com.flixclusive.data.watchlist.DefaultWatchlistRepository
import com.flixclusive.data.watchlist.WatchlistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WatchlistDataModule {
    @Binds
    internal abstract fun bindsWatchlistRepository(
        watchlistRepository: DefaultWatchlistRepository,
    ): WatchlistRepository

}