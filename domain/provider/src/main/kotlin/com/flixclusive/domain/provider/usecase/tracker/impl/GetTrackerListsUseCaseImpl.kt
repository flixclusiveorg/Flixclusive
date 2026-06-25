package com.flixclusive.domain.provider.usecase.tracker.impl

import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.repository.ProviderResponseWrapper
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerApiUseCase
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListsUseCase
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.tracker.TrackerList
import javax.inject.Inject

internal class GetTrackerListsUseCaseImpl @Inject constructor(
    private val getTrackerApi: GetTrackerApiUseCase
) : GetTrackerListsUseCase {
    override suspend fun invoke(
        providers: List<ProviderResponseWrapper>
    ): List<TrackerList> {
        return providers
            .mapNotNull { provider ->
                if (!provider.isTrackerEnabled) return@mapNotNull null

                val api = runCatching {
                    getTrackerApi(provider.id)
                }.onFailure {
                    errorLog(
                        "An error occurred while getting tracker API for provider ${provider.metadata?.name}: ${it.message}"
                    )
                    it.printStackTrace()
                    return@mapNotNull null
                }.getOrNull() ?: return@mapNotNull null

                runCatching {
                    if (!api.getFeatures().contains(TrackerFeature.LIST_MANAGEMENT)) return@mapNotNull null
                }.onFailure {
                    errorLog(
                        "An error occurred while checking tracker provider ${provider.metadata?.name} capabilities: ${it.message}"
                    )
                    it.printStackTrace()
                    return@mapNotNull null
                }

                runCatching {
                    if (!api.isAuthenticated()) return@mapNotNull null
                }.onFailure {
                    errorLog(
                        "An error occurred while checking tracker provider ${provider.metadata?.name} authentication status: ${it.message}"
                    )
                    it.printStackTrace()
                    return@mapNotNull null
                }

                api.getLists()
            }.flatten()
    }
}
