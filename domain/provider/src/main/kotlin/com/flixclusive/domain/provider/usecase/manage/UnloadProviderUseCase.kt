package com.flixclusive.domain.provider.usecase.manage

import com.flixclusive.model.provider.ProviderMetadata

/**
 * A use case for unloading a provider.
 * */
interface UnloadProviderUseCase {
    /**
     * Unloads a provider by its metadata.
     *
     * @param metadata the metadata of the provider to unload.
     * @param unloadFromPrefs whether to remove the provider from preferences. Defaults to true.
     *
     * @throws Throwable if the provider cannot be unloaded.
     */
    suspend operator fun invoke(
        metadata: ProviderMetadata,
        unloadFromPrefs: Boolean = true
    )
}
