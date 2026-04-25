package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetProviderMetadataUseCase
import com.flixclusive.model.provider.ProviderMetadata
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class GetProviderMetadataUseCaseImpl @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val userSessionDataStore: UserSessionDataStore,
) : GetProviderMetadataUseCase {
    override suspend fun invoke(id: String): ProviderMetadata? {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        return providerRepository.getProvider(id, userId)?.metadata
    }
}
