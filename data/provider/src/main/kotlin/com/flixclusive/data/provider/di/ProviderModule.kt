package com.flixclusive.data.provider.di

import com.flixclusive.data.provider.DefaultSourceLinksRepository
import com.flixclusive.data.provider.SourceLinksRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    @Singleton
    @Binds
    internal abstract fun bindsSourceLinksRepository(
        sourceLinksRepository: DefaultSourceLinksRepository,
    ): SourceLinksRepository

}