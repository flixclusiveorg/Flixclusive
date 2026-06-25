package com.flixclusive.domain.provider.usecase.manage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface InitializeProvidersUseCase {
    /**
     * A StateFlow that emits the loading state of the provider initialization.
     * */
    val isLoading: StateFlow<Boolean>

    /**
     * Initializes and loads all downloaded providers from local storage.
     *
     * *NOTE: This also initializes debug providers*
     *
     * @return A flow containing the results of the initialization operation.
     * */
    operator fun invoke(): Flow<ProviderResult>
}
