package com.flixclusive.domain.provider.di

import com.flixclusive.domain.provider.usecase.get.GetCatalogProvidersUseCase
import com.flixclusive.domain.provider.usecase.get.GetCrossMatchedMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetInstalledProviderUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderPluginUseCase
import com.flixclusive.domain.provider.usecase.get.GetRepositoryUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.domain.provider.usecase.get.GetTrackerProvidersUseCase
import com.flixclusive.domain.provider.usecase.get.impl.GetCatalogProvidersUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetCrossMatchedMediaMetadataUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetInstalledProviderUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetMediaLinksUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetMediaMetadataUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetNextEpisodeUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetProviderFromRemoteUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetProviderMetadataUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetProviderPluginUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetRepositoryUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetSeasonWithWatchProgressUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetTrackerProvidersUseCaseImpl
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
    abstract fun bindGetEpisodeUseCase(impl: GetNextEpisodeUseCaseImpl): GetNextEpisodeUseCase

    @Binds
    abstract fun bindGetMediaMetadataUseCase(impl: GetMediaMetadataUseCaseImpl): GetMediaMetadataUseCase

    @Binds
    abstract fun bindGetSeasonWithWatchProgressUseCase(
        impl: GetSeasonWithWatchProgressUseCaseImpl,
    ): GetSeasonWithWatchProgressUseCase

    @Binds
    abstract fun bindGetCatalogProvidersUseCase(impl: GetCatalogProvidersUseCaseImpl): GetCatalogProvidersUseCase

    @Binds
    abstract fun bindGetTrackerProvidersUseCase(impl: GetTrackerProvidersUseCaseImpl): GetTrackerProvidersUseCase

    @Binds
    abstract fun bindGetProviderPluginUseCase(impl: GetProviderPluginUseCaseImpl): GetProviderPluginUseCase

    @Binds
    abstract fun bindGetProviderMetadataUseCase(impl: GetProviderMetadataUseCaseImpl): GetProviderMetadataUseCase

    @Binds
    abstract fun bindGetInstalledProviderUseCase(impl: GetInstalledProviderUseCaseImpl): GetInstalledProviderUseCase

    @Binds
    abstract fun bindGetCrossMatchedMediaMetadataUseCase(
        impl: GetCrossMatchedMediaMetadataUseCaseImpl,
    ): GetCrossMatchedMediaMetadataUseCase
}
