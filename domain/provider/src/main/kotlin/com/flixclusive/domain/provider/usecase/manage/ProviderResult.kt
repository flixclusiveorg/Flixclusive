package com.flixclusive.domain.provider.usecase.manage

import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Status

sealed class ProviderResult {
    data class Success(
        val provider: ProviderMetadata,
    ) : ProviderResult()

    data class Failure(
        val provider: ProviderMetadata,
        val error: Throwable,
    ) : ProviderResult()
}
