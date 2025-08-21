package com.flixclusive.domain.provider.usecase.manage

import kotlinx.coroutines.flow.Flow

interface InitializeProvidersUseCase {
    /**
     * Initializes and loads all downloaded providers from local storage.
     *
     * *NOTE: This also initializes debug providers*
     *
     * @return A flow containing the results of the initialization operation.
     * */
    operator fun invoke(): Flow<LoadProviderResult>
}
