package com.flixclusive.data.provider.di

import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.repository.impl.CachedLinksRepositoryImpl
import com.flixclusive.data.provider.repository.impl.ProviderApiRepositoryImpl
import com.flixclusive.data.provider.repository.impl.ProviderRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProviderModule {
    @Singleton
    @Binds
    abstract fun provideCachedLinksRepository(
        cachedLinksRepository: CachedLinksRepositoryImpl
    ): CachedLinksRepository

    @Singleton
    @Binds
    abstract fun provideProviderApiRepository(
        providerApiRepository: ProviderApiRepositoryImpl
    ): ProviderApiRepository

    @Singleton
    @Binds
    abstract fun provideProviderRepository(
        providerRepository: ProviderRepositoryImpl
    ): ProviderRepository
}
