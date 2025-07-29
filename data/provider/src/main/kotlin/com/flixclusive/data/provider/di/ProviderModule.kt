package com.flixclusive.data.provider.di

import com.flixclusive.data.provider.DefaultProviderApiRepository
import com.flixclusive.data.provider.DefaultProviderRepository
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.data.provider.cache.CachedLinksRepository
import com.flixclusive.data.provider.cache.DefaultCachedLinksRepository
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
        cachedLinksRepository: DefaultCachedLinksRepository
    ): CachedLinksRepository

    @Singleton
    @Binds
    abstract fun provideProviderApiRepository(
        providerApiRepository: DefaultProviderApiRepository
    ): ProviderApiRepository

    @Singleton
    @Binds
    abstract fun provideProviderRepository(
        providerRepository: DefaultProviderRepository
    ): ProviderRepository
}
