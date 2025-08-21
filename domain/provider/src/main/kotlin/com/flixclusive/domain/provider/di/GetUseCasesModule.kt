package com.flixclusive.domain.provider.di

import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.get.GetRepositoryUseCase
import com.flixclusive.domain.provider.usecase.get.impl.GetMediaLinksUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetProviderFromRemoteUseCaseImpl
import com.flixclusive.domain.provider.usecase.get.impl.GetRepositoryUseCaseImpl
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
    @Singleton
    abstract fun bindGetRepositoryUseCase(impl: GetRepositoryUseCaseImpl): GetRepositoryUseCase
}
