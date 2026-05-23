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
    private val toggleJobs: MutableMap<ProviderCapability, Job> = mutableMapOf()

    override fun invoke(id: String, capability: ProviderCapability) {
        if (toggleJobs[capability]?.isActive == true) return

        toggleJobs[capability] = appDispatchers.ioScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            providerRepository.toggleCapability(id = id, ownerId = userId, capability = capability)
        }
    }
}
