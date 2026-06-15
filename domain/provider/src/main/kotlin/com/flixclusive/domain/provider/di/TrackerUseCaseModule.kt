package com.flixclusive.domain.provider.di

import com.flixclusive.domain.provider.usecase.tracker.GetTrackerApiUseCase
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListItemsUseCase
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListsUseCase
import com.flixclusive.domain.provider.usecase.tracker.SyncFromScrobblersUseCase
import com.flixclusive.domain.provider.usecase.tracker.SyncToScrobblersUseCase
import com.flixclusive.domain.provider.usecase.tracker.ToggleListItemOnTrackerListUseCase
import com.flixclusive.domain.provider.usecase.tracker.impl.GetTrackerApiUseCaseImpl
import com.flixclusive.domain.provider.usecase.tracker.impl.GetTrackerListItemsUseCaseImpl
import com.flixclusive.domain.provider.usecase.tracker.impl.GetTrackerListsUseCaseImpl
import com.flixclusive.domain.provider.usecase.tracker.impl.SyncFromScrobblersUseCaseImpl
import com.flixclusive.domain.provider.usecase.tracker.impl.SyncToScrobblersUseCaseImpl
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
    abstract fun bindGetTrackerListItemsUseCase(impl: GetTrackerListItemsUseCaseImpl): GetTrackerListItemsUseCase

    @Binds
    abstract fun bindToggleListItemOnTrackerListUseCase(
        impl: ToggleListItemOnTrackerListUseCaseImpl
    ): ToggleListItemOnTrackerListUseCase

    @Binds
    abstract fun bindGetTrackerApiUseCase(impl: GetTrackerApiUseCaseImpl): GetTrackerApiUseCase

    @Binds
    abstract fun bindSyncFromScrobblersUseCase(impl: SyncFromScrobblersUseCaseImpl): SyncFromScrobblersUseCase

    @Binds
    abstract fun bindSyncToScrobblersUseCase(impl: SyncToScrobblersUseCaseImpl): SyncToScrobblersUseCase
}
