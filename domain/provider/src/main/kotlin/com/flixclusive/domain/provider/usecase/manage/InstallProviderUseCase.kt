package com.flixclusive.domain.provider.usecase.manage

import com.flixclusive.model.provider.ProviderMetadata
import kotlinx.coroutines.flow.Flow
/**
 * Use case for installing a provider from a remote repository.
 */
interface InstallProviderUseCase {
    operator fun invoke(metadata: ProviderMetadata): Flow<ProviderResult>
}
