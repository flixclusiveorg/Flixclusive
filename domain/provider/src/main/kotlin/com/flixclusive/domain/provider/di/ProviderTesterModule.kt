package com.flixclusive.domain.provider.di

import com.flixclusive.domain.provider.testing.ProviderTester
import com.flixclusive.domain.provider.testing.impl.ProviderTesterImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProviderTesterModule {
    @Binds
    @Singleton
    abstract fun bindProviderTester(impl: ProviderTesterImpl): ProviderTester
}
