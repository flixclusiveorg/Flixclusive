package com.flixclusive.di

import com.flixclusive.data.api.TMDBApiService
import com.flixclusive.data.database.AppDatabase
import com.flixclusive.data.repository.ProvidersRepositoryImpl
import com.flixclusive.data.repository.TMDBRepositoryImpl
import com.flixclusive.data.repository.UserRepositoryImpl
import com.flixclusive.data.repository.VideoDataSourceRepositoryImpl
import com.flixclusive.data.repository.WatchHistoryRepositoryImpl
import com.flixclusive.data.repository.WatchlistRepositoryImpl
import com.flixclusive.domain.config.ConfigurationProvider
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.repository.ProvidersRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.repository.UserRepository
import com.flixclusive.domain.repository.VideoDataSourceRepository
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
    ): TMDBRepository = TMDBRepositoryImpl(
        tmdbApiService = apiService,
        configurationProvider = configurationProvider,
        ioDispatcher = ioDispatcher
    )

    // provide FilmsSourcesRepository
    @Provides
    @Singleton
    fun provideFilmSourcesRepository(
        tmdbRepository: TMDBRepository,
        providersRepository: ProvidersRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): VideoDataSourceRepository = VideoDataSourceRepositoryImpl(
        providersRepository = providersRepository,
        tmdbRepository = tmdbRepository,
        ioDispatcher = ioDispatcher
    )

    // provide WatchlistRepository
    @Provides
    @Singleton
    fun provideWatchlistRepository(
        appDatabase: AppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): WatchlistRepository {
        return WatchlistRepositoryImpl(
            watchlistDao = appDatabase.watchlistDao(),
            ioDispatcher = ioDispatcher
        )
    }

    // provide WatchHistoryRepository
    @Provides
    @Singleton
    fun provideWatchHistoryRepository(
        appDatabase: AppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): WatchHistoryRepository {
        return WatchHistoryRepositoryImpl(
            watchHistoryDao = appDatabase.watchHistoryDao(),
            ioDispatcher = ioDispatcher
        )
    }

    // provide UserRepository
    @Provides
    @Singleton
    fun provideUserRepository(
        appDatabase: AppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): UserRepository {
        return UserRepositoryImpl(
            userDao = appDatabase.userDao(),
            ioDispatcher = ioDispatcher
        )
    }

    // provide UserRepository
    @Provides
    @Singleton
    fun provideProvidersRepository(
        client: OkHttpClient,
        appSettingsManager: AppSettingsManager,
    ): ProvidersRepository {
        return ProvidersRepositoryImpl(
            client = client,
            appSettingsManager = appSettingsManager
        )
    }
}