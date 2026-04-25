package com.flixclusive.domain.provider.usecase.manage.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.ToggleProviderUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ToggleProviderUseCaseImpl @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers,
) : ToggleProviderUseCase {
    private var toggleJob: Job? = null
    override fun invoke(id: String) {
        if (toggleJob?.isActive == true) return

        toggleJob = appDispatchers.ioScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            providerRepository.toggleProvider(id = id, ownerId = userId)
        }
    }
}
