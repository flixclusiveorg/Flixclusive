package com.flixclusive.di

import com.flixclusive.data.usecase.HomeItemsProviderUseCaseImpl
import com.flixclusive.data.usecase.SeasonProviderUseCaseImpl
import com.flixclusive.data.usecase.VideoDataProviderUseCaseImpl
import com.flixclusive.data.usecase.WatchHistoryItemManagerUseCaseImpl
import com.flixclusive.data.usecase.WatchlistItemManagerUseCaseImpl
import com.flixclusive.domain.repository.ConsumetRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.repository.WatchlistRepository
import com.flixclusive.domain.usecase.HomeItemsProviderUseCase
import com.flixclusive.domain.usecase.SeasonProviderUseCase
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.domain.usecase.WatchHistoryItemManagerUseCase
import com.flixclusive.domain.usecase.WatchlistItemManagerUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    @Provides
    fun provideVideoDataProviderUseCase(
        consumetRepository: ConsumetRepository,
        tmdbRepository: TMDBRepository
    ): VideoDataProviderUseCase
        = VideoDataProviderUseCaseImpl(consumetRepository, tmdbRepository)

    @Provides
    fun provideSeasonProviderUseCase(
        tmdbRepository: TMDBRepository
    ): SeasonProviderUseCase
        = SeasonProviderUseCaseImpl(tmdbRepository)

    @Provides
    fun provideWatchHistoryItemManagerUseCase(
        watchHistoryRepository: WatchHistoryRepository,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): WatchHistoryItemManagerUseCase
        = WatchHistoryItemManagerUseCaseImpl(watchHistoryRepository, defaultDispatcher)

    @Provides
    fun provideWatchlistItemManagerUseCase(
        watchlistRepository: WatchlistRepository
    ): WatchlistItemManagerUseCase
        = WatchlistItemManagerUseCaseImpl(watchlistRepository)

    @Provides
    fun provideHomeItemsProviderUseCase(
        tmdbRepository: TMDBRepository,
        watchHistoryRepository: WatchHistoryRepository
    ): HomeItemsProviderUseCase
        = HomeItemsProviderUseCaseImpl(tmdbRepository, watchHistoryRepository)
}