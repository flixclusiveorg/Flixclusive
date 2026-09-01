package com.flixclusive.domain.provider.usecase.tracker.impl

import com.flixclusive.data.provider.repository.TrackerListRepository
import com.flixclusive.domain.provider.usecase.get.GetTrackerProvidersUseCase
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListsUseCase
import com.flixclusive.domain.provider.usecase.tracker.TrackerLists
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class GetTrackerListsUseCaseImpl @Inject constructor(
    private val getTrackerProviders: GetTrackerProvidersUseCase,
    private val trackerListRepository: TrackerListRepository,
) : GetTrackerListsUseCase {
    override fun invoke(): Flow<TrackerLists> =
        getTrackerListsFlow(
            getTrackerProviders = getTrackerProviders,
            repository = trackerListRepository,
        )
}
