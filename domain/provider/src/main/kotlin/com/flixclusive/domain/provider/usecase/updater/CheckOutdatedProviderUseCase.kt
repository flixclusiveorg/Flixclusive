package com.flixclusive.domain.provider.usecase.updater

import com.flixclusive.model.provider.ProviderMetadata

interface CheckOutdatedProviderUseCase {
    /**
     * Checks if any of the downloaded providers are outdated.
     *
     * @return A list of results indicating whether each provider is outdated or up to date.
     */
    suspend operator fun invoke(): List<CheckOutdatedProviderResult>

    /**
     * Checks if the given provider is outdated.
     *
     * @param metadata The metadata of the provider to check.
     * @return true if the provider is outdated, false otherwise.
     */
    suspend operator fun invoke(metadata: ProviderMetadata): Boolean
}

/**
 * Represents the result of checking if a provider is outdated.
 *
 * @property metadata The metadata of the provider that was checked.
 * */
sealed class CheckOutdatedProviderResult(val metadata: ProviderMetadata) {
    /**
     * Indicates that the provider is outdated.
     * */
    class Outdated(metadata: ProviderMetadata) : CheckOutdatedProviderResult(metadata)
    /**
     * Indicates that the provider is up to date.
     * */
    class UpToDate(metadata: ProviderMetadata) : CheckOutdatedProviderResult(metadata)
    /**
     * Indicates that an error occurred while checking the provider.
     *
     * @param error The error that occurred.
     * */
    class Error(val error: Throwable, metadata: ProviderMetadata) : CheckOutdatedProviderResult(metadata)
}
