package com.flixclusive.core.database.di

import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.dao.SearchHistoryDao
import com.flixclusive.core.database.dao.UserDao
import com.flixclusive.core.database.dao.WatchHistoryDao
import com.flixclusive.core.database.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaoModule {
    @Provides
    fun providesUserDao(
        database: AppDatabase,
    ): UserDao = database.userDao()

    @Provides
    fun providesWatchlistDao(
        database: AppDatabase,
    ): WatchlistDao = database.watchlistDao()

    @Provides
    fun providesWatchHistoryDao(
        database: AppDatabase,
    ): WatchHistoryDao = database.watchHistoryDao()

    @Provides
    fun providesSearchHistoryDao(
        database: AppDatabase,
    ): SearchHistoryDao = database.searchHistoryDao()
}