package com.flixclusive.config.di

import com.flixclusive.config.BuildConfigProviderImpl
import com.flixclusive.core.common.config.BuildConfigProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BuildConfigModule {
    @Binds
    @Singleton
    abstract fun bindBuildConfigProvider(
        buildConfigProviderImpl: BuildConfigProviderImpl
    ): BuildConfigProvider
}
