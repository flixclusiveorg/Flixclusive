package com.flixclusive.domain.provider.usecase.tracker.impl

import android.content.Context
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.repository.ProviderResponseWrapper
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListsUseCase
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.tracker.TrackerList
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class GetTrackerListsUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : GetTrackerListsUseCase {
    override suspend fun invoke(
        providers: List<ProviderResponseWrapper>
    ): List<TrackerList> {
        return providers.mapNotNull { provider ->
            if (!provider.isEnabled) return@mapNotNull null

            val api = safeCall {
                provider.plugin?.getTrackerApi(context)
            } ?: return@mapNotNull null

            runCatching {
                if (!api.getFeatures().contains(TrackerFeature.LIST_MANAGEMENT)) return@mapNotNull null
            }.onFailure {
                errorLog("An error occurred while checking tracker provider ${provider.metadata?.name} capabilities: ${it.message}")
                it.printStackTrace()
                return@mapNotNull null
            }

            if (!api.isAuthenticated()) return@mapNotNull null

            api.getLists()
        }.flatten()
    }
}
