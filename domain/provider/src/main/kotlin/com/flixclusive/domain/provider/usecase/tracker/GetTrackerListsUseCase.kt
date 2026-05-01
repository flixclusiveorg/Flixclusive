package com.flixclusive.domain.provider.usecase.tracker

import com.flixclusive.data.provider.repository.ProviderResponseWrapper
import com.flixclusive.provider.tracker.TrackerList

interface GetTrackerListsUseCase {
    suspend operator fun invoke(
        providers: List<ProviderResponseWrapper>
    ): List<TrackerList>
}
