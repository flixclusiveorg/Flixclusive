package com.flixclusive.di

import com.flixclusive.data.api.ConsumetApiService
import com.flixclusive.data.api.TMDBApiService
import com.flixclusive.data.database.watch_history.WatchHistoryDatabase
import com.flixclusive.data.database.watchlist.WatchlistDatabase
import com.flixclusive.data.repository.ConsumetRepositoryImpl
import com.flixclusive.data.repository.TMDBRepositoryImpl
import com.flixclusive.data.repository.WatchHistoryRepositoryImpl
import com.flixclusive.data.repository.WatchlistRepositoryImpl
import com.flixclusive.domain.firebase.ConfigurationProvider
import com.flixclusive.domain.repository.ConsumetRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.repository.WatchlistRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
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

    // provide ConsumetRepository
    @Provides
    @Singleton
    fun provideConsumetRepository(
        consumetApiService: ConsumetApiService,
        configurationProvider: ConfigurationProvider,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ConsumetRepository {
        return ConsumetRepositoryImpl(
            consumetApiService = consumetApiService,
            configurationProvider = configurationProvider,
            ioDispatcher = ioDispatcher
        )
    }

    // provide WatchlistRepository
    @Provides
    @Singleton
    fun provideWatchlistRepository(
        watchlistDatabase: WatchlistDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): WatchlistRepository {
        return WatchlistRepositoryImpl(watchlistDatabase.watchlistDao(), ioDispatcher)
    }

    // provide WatchHistoryRepository
    @Provides
    @Singleton
    fun provideWatchHistoryRepository(
        watchHistoryDatabase: WatchHistoryDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): WatchHistoryRepository {
        return WatchHistoryRepositoryImpl(watchHistoryDatabase.watchHistoryDao(), ioDispatcher)
    }
}