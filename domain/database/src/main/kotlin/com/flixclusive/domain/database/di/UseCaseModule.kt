package com.flixclusive.domain.database.di

import com.flixclusive.domain.database.usecase.SetWatchProgressUseCase
import com.flixclusive.domain.database.usecase.ToggleWatchProgressStatusUseCase
import com.flixclusive.domain.database.usecase.ToggleWatchlistStatusUseCase
import com.flixclusive.domain.database.usecase.impl.SetWatchProgressUseCaseImpl
import com.flixclusive.domain.database.usecase.impl.ToggleWatchProgressStatusUseCaseImpl
import com.flixclusive.domain.database.usecase.impl.ToggleWatchlistStatusUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UseCaseModule {
    @Binds
    abstract fun bindSetWatchProgressUseCase(
        impl: SetWatchProgressUseCaseImpl
    ): SetWatchProgressUseCase

    @Binds
    abstract fun bindToggleWatchlistStatusUseCase(
        impl: ToggleWatchlistStatusUseCaseImpl
    ): ToggleWatchlistStatusUseCase

    @Binds
    abstract fun bindToggleWatchProgressStatusUseCase(
        impl: ToggleWatchProgressStatusUseCaseImpl,
    ): ToggleWatchProgressStatusUseCase
}
