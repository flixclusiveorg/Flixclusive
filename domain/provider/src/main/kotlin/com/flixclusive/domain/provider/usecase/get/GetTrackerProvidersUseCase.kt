package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.common.domain.Async
import com.flixclusive.data.provider.repository.ProviderResponseWrapper
import kotlinx.coroutines.flow.Flow

interface GetTrackerProvidersUseCase {
    operator fun invoke(): Flow<Async<List<ProviderResponseWrapper>>>
}
