package com.flixclusive.domain.provider.di

import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListsUseCase
import com.flixclusive.domain.provider.usecase.tracker.ToggleListItemOnTrackerListUseCase
import com.flixclusive.domain.provider.usecase.tracker.impl.GetTrackerListsUseCaseImpl
import com.flixclusive.domain.provider.usecase.tracker.impl.ToggleListItemOnTrackerListUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TrackerUseCaseModule {
    @Binds
    abstract fun bindGetTrackerLibrariesUseCase(impl: GetTrackerListsUseCaseImpl): GetTrackerListsUseCase

    @Binds
    abstract fun bindToggleListItemOnTrackerListUseCase(impl: ToggleListItemOnTrackerListUseCaseImpl): ToggleListItemOnTrackerListUseCase
}
