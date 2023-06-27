package com.flixclusive.di

import android.app.Application
import com.flixclusive.data.database.watch_history.WatchHistoryDatabase
import com.flixclusive.data.database.watchlist.WatchlistDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideWatchlistDatabase(
        application: Application
    ) : WatchlistDatabase {
        return WatchlistDatabase.getInstance(application)
    }

    @Provides
    @Singleton
    fun provideWatchHistoryDatabase(
        application: Application
    ) : WatchHistoryDatabase {
        return WatchHistoryDatabase.getInstance(application)
    }
}