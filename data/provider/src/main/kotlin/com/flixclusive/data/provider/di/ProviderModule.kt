package com.flixclusive.data.provider.di

import com.flixclusive.data.provider.DefaultProviderRepository
import com.flixclusive.data.provider.DefaultSourceLinksRepository
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.data.provider.SourceLinksRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    @Binds
    internal abstract fun bindsProviderRepository(
        providerRepository: DefaultProviderRepository,
    ): ProviderRepository

    @Binds
    internal abstract fun bindsSourceLinksRepository(
        sourceLinksRepository: DefaultSourceLinksRepository,
    ): SourceLinksRepository

}