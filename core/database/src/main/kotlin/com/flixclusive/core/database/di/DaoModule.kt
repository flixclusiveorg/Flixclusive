package com.flixclusive.core.database.di

import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.dao.SearchHistoryDao
import com.flixclusive.core.database.dao.UserDao
import com.flixclusive.core.database.dao.library.LibraryListDao
import com.flixclusive.core.database.dao.library.LibraryListItemDao
import com.flixclusive.core.database.dao.provider.InstalledProviderDao
import com.flixclusive.core.database.dao.provider.InstalledRepositoryDao
import com.flixclusive.core.database.dao.watched.EpisodeProgressDao
import com.flixclusive.core.database.dao.watched.MovieProgressDao
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
    fun providesMovieProgressDao(database: AppDatabase): MovieProgressDao = database.movieProgressDao()

    @Provides
    fun providesEpisodeProgressDao(database: AppDatabase): EpisodeProgressDao = database.episodeProgressDao()

    @Provides
    fun providesSearchHistoryDao(database: AppDatabase): SearchHistoryDao = database.searchHistoryDao()

    @Provides
    fun providesLibraryListDao(database: AppDatabase): LibraryListDao = database.libraryListDao()

    @Provides
    fun providesLibraryListItemDao(database: AppDatabase): LibraryListItemDao = database.libraryListItemDao()

    @Provides
    fun providesRepositoryDao(database: AppDatabase): InstalledRepositoryDao = database.repositoryDao()

    @Provides
    fun providesInstalledProviderDao(database: AppDatabase): InstalledProviderDao = database.installedProviderDao()
}
