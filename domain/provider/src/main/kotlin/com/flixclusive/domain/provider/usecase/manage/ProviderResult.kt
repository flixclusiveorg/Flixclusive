package com.flixclusive.domain.provider.usecase.manage

import com.flixclusive.model.provider.ProviderMetadata

sealed class ProviderResult {
    data class Success(
        val provider: ProviderMetadata,
    ) : ProviderResult()

    data class Failure(
        val provider: ProviderMetadata,
        val error: Throwable,
    ) : ProviderResult()
}
