package com.flixclusive.domain.provider.usecase.updater.impl

import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderResult
import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderUseCase
import com.flixclusive.domain.provider.util.Constants
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import javax.inject.Inject

internal class CheckOutdatedProviderUseCaseImpl
    @Inject
    constructor(
        private val providerRepository: ProviderRepository,
        private val getProviderFromRemoteUseCase: GetProviderFromRemoteUseCase,
    ) : CheckOutdatedProviderUseCase {
        override suspend fun invoke(): List<CheckOutdatedProviderResult> {
            return providerRepository.getOrderedProviders().map { metadata ->
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
            if (id.endsWith(Constants.PROVIDER_DEBUG)) return false

            val provider = providerRepository.getProvider(id) ?: return false

            val manifest = provider.manifest
            if (manifest.updateUrl == null || manifest.updateUrl.equals("")) {
                return false
            }

            val repository = metadata.repositoryUrl.toValidRepositoryLink()
            val response = getProviderFromRemoteUseCase(repository, id)

            if (response is Resource.Failure) {
                throw ExceptionWithUiText(response.error)
            }

            if (response.data == null) {
                throw ExceptionWithUiText(
                    uiText = UiText.from(R.string.provider_not_found_message),
                    cause = NullPointerException(),
                )
            }

            val updatedMetadata = response.data!!

            return manifest.versionCode < updatedMetadata.versionCode
        }
    }
