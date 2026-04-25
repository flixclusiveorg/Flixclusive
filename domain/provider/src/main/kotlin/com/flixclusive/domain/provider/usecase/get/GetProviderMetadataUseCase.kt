package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.model.provider.ProviderMetadata

interface GetProviderMetadataUseCase {
    suspend operator fun invoke(id: String): ProviderMetadata?
}
