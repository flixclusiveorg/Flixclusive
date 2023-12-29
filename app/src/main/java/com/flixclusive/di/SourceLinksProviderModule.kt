package com.flixclusive.di

import com.flixclusive.data.usecase.SourceLinksProviderUseCaseImpl
import com.flixclusive.domain.repository.ProvidersRepository
import com.flixclusive.domain.repository.SourceLinksRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.usecase.SourceLinksProviderUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SourceLinksProviderModule {
    @Provides
    @Singleton
    fun provideVideoDataProviderUseCase(
        sourceLinksRepository: SourceLinksRepository,
        providersRepository: ProvidersRepository,
        tmdbRepository: TMDBRepository
    ): SourceLinksProviderUseCase
            = SourceLinksProviderUseCaseImpl(
        sourceLinksRepository = sourceLinksRepository,
        providersRepository = providersRepository,
        tmdbRepository = tmdbRepository
    )
}