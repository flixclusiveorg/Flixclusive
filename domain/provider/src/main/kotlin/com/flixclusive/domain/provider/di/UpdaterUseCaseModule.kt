package com.flixclusive.domain.provider.di

import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.impl.CheckOutdatedProviderUseCaseImpl
import com.flixclusive.domain.provider.usecase.updater.impl.UpdateProviderUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UpdaterUseCaseModule {
    @Binds
    @Singleton
    abstract fun bindCheckOutdatedProviderUseCase(impl: CheckOutdatedProviderUseCaseImpl): CheckOutdatedProviderUseCase

    @Binds
    @Singleton
    abstract fun bindUpdateProviderUseCase(impl: UpdateProviderUseCaseImpl): UpdateProviderUseCase
}
