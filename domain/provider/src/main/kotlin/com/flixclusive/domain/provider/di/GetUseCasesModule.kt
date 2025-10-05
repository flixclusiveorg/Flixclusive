package com.flixclusive.domain.provider.di

import com.flixclusive.domain.provider.usecase.get.GetEpisodeUseCase
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.get.GetRepositoryUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.domain.provider.usecase.get.impl.GetEpisodeUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetFilmMetadataUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetMediaLinksUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetProviderFromRemoteUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetRepositoryUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetSeasonWithWatchProgressUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GetUseCasesModule {
    @Binds
    @Singleton
    abstract fun bindGetMediaLinksUseCase(impl: GetMediaLinksUseCaseImpl): GetMediaLinksUseCase

    @Binds
    @Singleton
    abstract fun bindGetProviderFromRemoteUseCase(impl: GetProviderFromRemoteUseCaseImpl): GetProviderFromRemoteUseCase

    @Binds
    abstract fun bindGetRepositoryUseCase(impl: GetRepositoryUseCaseImpl): GetRepositoryUseCase

    @Binds
    abstract fun bindGetEpisodeUseCase(impl: GetEpisodeUseCaseImpl): GetEpisodeUseCase

    @Binds
    abstract fun bindGetFilmMetadataUseCase(impl: GetFilmMetadataUseCaseImpl): GetFilmMetadataUseCase

    @Binds
    abstract fun bindGetSeasonWithWatchProgressUseCase(
        impl: GetSeasonWithWatchProgressUseCaseImpl,
    ): GetSeasonWithWatchProgressUseCase
}
