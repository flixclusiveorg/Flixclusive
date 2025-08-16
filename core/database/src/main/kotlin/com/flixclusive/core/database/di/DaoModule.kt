package com.flixclusive.core.database.di

import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.dao.DBFilmDao
import com.flixclusive.core.database.dao.EpisodeProgressDao
import com.flixclusive.core.database.dao.LibraryListDao
import com.flixclusive.core.database.dao.LibraryListItemDao
import com.flixclusive.core.database.dao.MovieProgressDao
import com.flixclusive.core.database.dao.SearchHistoryDao
import com.flixclusive.core.database.dao.UserDao
import com.flixclusive.core.database.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaoModule {
    @Provides
    fun providesUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun providesWatchlistDao(database: AppDatabase): WatchlistDao = database.watchlistDao()

    @Provides
    fun providesMovieProgressDao(database: AppDatabase): MovieProgressDao = database.movieProgressDao()

    @Provides
    fun providesEpisodeProgressDao(database: AppDatabase): EpisodeProgressDao = database.episodeProgressDao()

    @Provides
    fun providesFilmsDao(database: AppDatabase): DBFilmDao = database.filmsDao()

    @Provides
    fun providesSearchHistoryDao(database: AppDatabase): SearchHistoryDao = database.searchHistoryDao()

    @Provides
    fun providesLibraryListDao(database: AppDatabase): LibraryListDao = database.libraryListDao()

    @Provides
    fun providesLibraryListItemDao(database: AppDatabase): LibraryListItemDao = database.libraryListItemDao()
}
