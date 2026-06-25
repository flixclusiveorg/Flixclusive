package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetProviderPluginUseCase
import com.flixclusive.provider.ProviderPlugin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class GetProviderPluginUseCaseImpl @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository
) : GetProviderPluginUseCase {
    override suspend fun invoke(id: String): ProviderPlugin? {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        return providerRepository.getProvider(id, userId)?.plugin
    }
}
