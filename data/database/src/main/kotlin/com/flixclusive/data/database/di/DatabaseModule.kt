package com.flixclusive.data.database.di

import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.SearchHistoryRepository
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.data.database.repository.impl.LibraryListRepositoryImpl
import com.flixclusive.data.database.repository.impl.SearchHistoryRepositoryImpl
import com.flixclusive.data.database.repository.impl.UserRepositoryImpl
import com.flixclusive.data.database.repository.impl.WatchProgressRepositoryImpl
import com.flixclusive.data.database.repository.impl.WatchlistRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DatabaseModule {
    @Singleton
    @Binds
    internal abstract fun bindsWatchlistRepository(watchlistRepository: WatchlistRepositoryImpl): WatchlistRepository

    @Singleton
    @Binds
    internal abstract fun bindsWatchProgressRepository(
        watchHistoryRepository: WatchProgressRepositoryImpl,
    ): WatchProgressRepository

    @Singleton
    @Binds
    abstract fun bindsUserRepository(userRepository: UserRepositoryImpl): UserRepository

    @Singleton
    @Binds
    internal abstract fun bindsSearchHistoryRepository(
        searchHistoryRepository: SearchHistoryRepositoryImpl,
    ): SearchHistoryRepository

    @Singleton
    @Binds
    abstract fun bindsLibraryListRepository(libraryListRepository: LibraryListRepositoryImpl): LibraryListRepository
}
