package com.flixclusive.domain.provider.di

import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.impl.InitializeProvidersUseCaseImpl
import com.flixclusive.domain.provider.usecase.manage.impl.LoadProviderUseCaseImpl
import com.flixclusive.domain.provider.usecase.manage.impl.UnloadProviderUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ManageUseCasesModule {
    @Binds
    @Singleton
    abstract fun bindInitializeProviderUseCase(impl: InitializeProvidersUseCaseImpl): InitializeProvidersUseCase

    @Binds
    @Singleton
    abstract fun bindLoadProviderUseCase(impl: LoadProviderUseCaseImpl): LoadProviderUseCase

    @Binds
    @Singleton
    abstract fun bindUnloadProviderUseCase(impl: UnloadProviderUseCaseImpl): UnloadProviderUseCase
}
