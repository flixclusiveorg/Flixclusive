package com.flixclusive.domain.provider.usecase.updater.impl

import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderResult
import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderUseCase
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class CheckOutdatedProviderUseCaseImpl @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val getProviderFromRemoteUseCase: GetProviderFromRemoteUseCase,
) : CheckOutdatedProviderUseCase {
    override suspend fun invoke(): List<CheckOutdatedProviderResult> {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val providers = providerRepository.getProviders(userId)

        return providers.mapNotNull { provider ->
            val metadata = provider.metadata ?: return@mapNotNull null

            try {
                if (invoke(metadata)) {
                    CheckOutdatedProviderResult.Outdated(metadata)
                } else {
                    CheckOutdatedProviderResult.UpToDate(metadata)
                }
            } catch (e: Throwable) {
                CheckOutdatedProviderResult.Error(
                    metadata = metadata,
                    error = e,
                )
            }
        }
    }

    override suspend fun invoke(metadata: ProviderMetadata): Boolean {
        val id = metadata.id
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()

        val provider = providerRepository.getProvider(id, userId) ?: return false
        val isDebug = providerRepository.getProvider(id, userId)?.isDebug ?: false
        if (isDebug) return false

        val manifest = provider.plugin?.manifest
        if (manifest?.updateUrl == null || manifest.updateUrl.equals("")) {
            return false
        }

        val repository = metadata.repositoryUrl.toValidRepositoryLink()
        val updatedMetadata = getProviderFromRemoteUseCase(repository, id)

        return manifest.versionCode < updatedMetadata.versionCode
    }
}
