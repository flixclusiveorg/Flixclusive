package com.flixclusive.domain.provider.usecase.manage.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.ToggleCapabilityUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ToggleCapabilityUseCaseImpl @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers,
) : ToggleCapabilityUseCase {
    private val toggleJobs: MutableMap<String, Job> = mutableMapOf()

    private fun getJobId(providerId: String, capability: ProviderCapability): String {
        return "$providerId-${capability.name}"
    }

    override fun invoke(id: String, capability: ProviderCapability) {
        val jobId = getJobId(id, capability)
        if (toggleJobs[jobId]?.isActive == true) return

        toggleJobs[jobId] = appDispatchers.ioScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val newToggleState = providerRepository
                .getProvider(id = id, ownerId = userId)
                ?.let {
                    when (capability) {
                        ProviderCapability.CATALOG -> !it.isCatalogEnabled
                        ProviderCapability.CROSS_MATCH -> !it.isCrossMatchEnabled
                        ProviderCapability.MEDIA_LINK -> !it.isMediaLinkEnabled
                        ProviderCapability.METADATA -> !it.isMetadataEnabled
                        ProviderCapability.SEARCH -> !it.isSearchEnabled
                        ProviderCapability.TRACKER -> !it.isTrackerEnabled
                    }
                } ?: true

            providerRepository.setCapabilityEnabled(
                id = id,
                ownerId = userId,
                capability = capability,
                enabled = newToggleState
            )
        }
    }

    override fun invoke(
        id: String,
        capability: ProviderCapability,
        enabled: Boolean
    ) {
        val jobId = getJobId(id, capability)
        if (toggleJobs[jobId]?.isActive == true) return

        toggleJobs[jobId] = appDispatchers.ioScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            providerRepository.setCapabilityEnabled(
                id = id,
                ownerId = userId,
                capability = capability,
                enabled = enabled
            )
        }
    }
}
