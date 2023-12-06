package com.flixclusive.di

import com.flixclusive.data.api.TMDBApiService
import com.flixclusive.data.database.AppDatabase
import com.flixclusive.data.repository.FilmSourcesRepositoryImpl
import com.flixclusive.data.repository.TMDBRepositoryImpl
import com.flixclusive.data.repository.UserRepositoryImpl
import com.flixclusive.data.repository.WatchHistoryRepositoryImpl
import com.flixclusive.data.repository.WatchlistRepositoryImpl
import com.flixclusive.domain.config.ConfigurationProvider
import com.flixclusive.domain.repository.FilmSourcesRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.repository.UserRepository
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.repository.WatchlistRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // provide TMDBRepository
    @Provides
    @Singleton
    fun provideTMDBRepository(
        apiService: TMDBApiService,
        configurationProvider: ConfigurationProvider,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): TMDBRepository {
        return TMDBRepositoryImpl(
            tmdbApiService = apiService,
            configurationProvider = configurationProvider,
            ioDispatcher = ioDispatcher
        )
    }

    // provide FilmsSourcesRepository
    @Provides
    @Singleton
    fun provideFilmSourcesRepository(
        client: OkHttpClient,
        tmdbRepository: TMDBRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): FilmSourcesRepository {
        return FilmSourcesRepositoryImpl(
            client = client,
            tmdbRepository = tmdbRepository,
            ioDispatcher = ioDispatcher
        )
    }

    // provide WatchlistRepository
    @Provides
    @Singleton
    fun provideWatchlistRepository(
        appDatabase: AppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): WatchlistRepository {
        return WatchlistRepositoryImpl(appDatabase.watchlistDao(), ioDispatcher)
    }

    // provide WatchHistoryRepository
    @Provides
    @Singleton
    fun provideWatchHistoryRepository(
        appDatabase: AppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): WatchHistoryRepository {
        return WatchHistoryRepositoryImpl(appDatabase.watchHistoryDao(), ioDispatcher)
    }

    // provide UserRepository
    @Provides
    @Singleton
    fun provideUserRepository(
        appDatabase: AppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): UserRepository {
        return UserRepositoryImpl(appDatabase.userDao(), ioDispatcher)
    }
}