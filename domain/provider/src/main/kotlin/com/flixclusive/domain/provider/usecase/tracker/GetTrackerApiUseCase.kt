package com.flixclusive.domain.provider.usecase.tracker

import com.flixclusive.provider.capability.TrackerProviderApi

interface GetTrackerApiUseCase {
    suspend operator fun invoke(id: String): TrackerProviderApi
}
