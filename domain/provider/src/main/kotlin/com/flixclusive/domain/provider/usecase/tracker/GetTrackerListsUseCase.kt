package com.flixclusive.domain.provider.usecase.tracker

import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.tracker.TrackerList
import kotlinx.coroutines.flow.Flow

data class TrackerListAndProvider(
    val list: TrackerList,
    val provider: ProviderMetadata,
    val containsMedia: Boolean = false,
)

data class TrackerLists(
    val lists: List<TrackerListAndProvider> = emptyList(),
    val errors: List<ProviderWithThrowable> = emptyList(),
    val isLoading: Boolean = false,
)

interface GetTrackerListsUseCase {
    operator fun invoke(): Flow<TrackerLists>
}
