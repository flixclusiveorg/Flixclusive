package com.flixclusive.domain.provider.di

import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListsForMediaUseCase
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListsUseCase
import com.flixclusive.domain.provider.usecase.tracker.SyncFromScrobblersUseCase
import com.flixclusive.domain.provider.usecase.tracker.SyncToScrobblersUseCase
import com.flixclusive.domain.provider.usecase.tracker.impl.GetTrackerListsForMediaUseCaseImpl
import com.flixclusive.domain.provider.usecase.tracker.impl.GetTrackerListsUseCaseImpl
import com.flixclusive.domain.provider.usecase.tracker.impl.SyncFromScrobblersUseCaseImpl
import com.flixclusive.domain.provider.usecase.tracker.impl.SyncToScrobblersUseCaseImpl
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
    abstract fun bindGetTrackerListsForMediaUseCase(
        impl: GetTrackerListsForMediaUseCaseImpl
    ): GetTrackerListsForMediaUseCase

    @Binds
    abstract fun bindSyncFromScrobblersUseCase(impl: SyncFromScrobblersUseCaseImpl): SyncFromScrobblersUseCase

    @Binds
    abstract fun bindSyncToScrobblersUseCase(impl: SyncToScrobblersUseCaseImpl): SyncToScrobblersUseCase
}
