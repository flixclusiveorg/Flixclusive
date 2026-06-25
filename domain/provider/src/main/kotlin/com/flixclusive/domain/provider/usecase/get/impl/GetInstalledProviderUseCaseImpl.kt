package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetInstalledProviderUseCase
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class GetInstalledProviderUseCaseImpl @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository
) : GetInstalledProviderUseCase {
    override suspend fun invoke(id: String): InstalledProvider? {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        return providerRepository.getProvider(id, userId)?.provider
    }
}
