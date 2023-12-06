package com.flixclusive.di

import com.flixclusive.data.usecase.FilmProviderUseCaseImpl
import com.flixclusive.data.usecase.HomeItemsProviderUseCaseImpl
import com.flixclusive.data.usecase.ModifyProvidersUseCaseImpl
import com.flixclusive.data.usecase.SeasonProviderUseCaseImpl
import com.flixclusive.data.usecase.VideoDataProviderUseCaseImpl
import com.flixclusive.data.usecase.WatchHistoryItemManagerUseCaseImpl
import com.flixclusive.data.usecase.WatchlistItemManagerUseCaseImpl
import com.flixclusive.domain.config.ConfigurationProvider
import com.flixclusive.domain.repository.FilmSourcesRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.repository.WatchlistRepository
import com.flixclusive.domain.usecase.FilmProviderUseCase
import com.flixclusive.domain.usecase.HomeItemsProviderUseCase
import com.flixclusive.domain.usecase.ModifyProvidersUseCase
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
    fun provideModifyProvidersUseCase(
        filmSourcesRepository: FilmSourcesRepository
    ): ModifyProvidersUseCase
        = ModifyProvidersUseCaseImpl(filmSourcesRepository)

    @Provides
    fun provideVideoDataProviderUseCase(
        filmSourcesRepository: FilmSourcesRepository,
        tmdbRepository: TMDBRepository
    ): VideoDataProviderUseCase
        = VideoDataProviderUseCaseImpl(filmSourcesRepository, tmdbRepository)

    @Provides
    fun provideSeasonProviderUseCase(
        tmdbRepository: TMDBRepository
    ): SeasonProviderUseCase
        = SeasonProviderUseCaseImpl(tmdbRepository)

    @Provides
    fun provideFilmProviderUseCase(
        tmdbRepository: TMDBRepository
    ): FilmProviderUseCase
        = FilmProviderUseCaseImpl(tmdbRepository)

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
        watchHistoryRepository: WatchHistoryRepository,
        configurationProvider: ConfigurationProvider
    ): HomeItemsProviderUseCase = HomeItemsProviderUseCaseImpl(
        tmdbRepository = tmdbRepository,
        watchHistoryRepository = watchHistoryRepository,
        configurationProvider = configurationProvider
    )
}