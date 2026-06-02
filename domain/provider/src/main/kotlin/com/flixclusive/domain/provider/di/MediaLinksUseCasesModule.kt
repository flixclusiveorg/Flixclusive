package com.flixclusive.domain.provider.di

import com.flixclusive.domain.provider.usecase.links.TestMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.links.impl.TestMediaLinksUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MediaLinksUseCasesModule {
    @Binds
    @Singleton
    abstract fun bindTestMediaLinksUseCase(impl: TestMediaLinksUseCaseImpl): TestMediaLinksUseCase
}
