package com.flixclusive.domain.provider.usecase.updater

import com.flixclusive.model.provider.ProviderMetadata

/**
 * A use case for making providers up to date.
 * */
interface UpdateProviderUseCase {
    /**
     * Updates the given provider to the latest version.
     *
     * @param provider The provider to update.
     *
     * @return A boolean indicating whether the update was successful or not.
     * */
    suspend operator fun invoke(provider: ProviderMetadata): Boolean

    /**
     * Updates the given list of providers to the latest version.
     *
     * @param providers The list of providers to update.
     * */
    suspend operator fun invoke(providers: List<ProviderMetadata>): ProviderUpdateResult

    companion object {
        const val NOTIFICATION_ID = "provider_update_notification"
        const val NOTIFICATION_NAME = "Flixclusive Provider Updates"
    }
}

/**
 * Represents a partial update operation where some providers were successfully updated,
 * while others failed.
 *
 * @property success The list of successfully updated providers.
 * @property failed The list of pairs containing the provider metadata and the associated error
 *                  for those that failed to update.
 */
data class ProviderUpdateResult(
    val success: List<ProviderMetadata>,
    val failed: List<Pair<ProviderMetadata, Throwable?>>,
)
