package com.flixclusive.data.provider.di

import com.flixclusive.data.provider.cache.CachedLinksRepository
import com.flixclusive.data.provider.cache.DefaultCachedLinksRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CachedLinksModule {
    @Singleton
    @Binds
    abstract fun provideCachedLinksRepository(
        cachedLinksRepository: DefaultCachedLinksRepository
    ): CachedLinksRepository
}
