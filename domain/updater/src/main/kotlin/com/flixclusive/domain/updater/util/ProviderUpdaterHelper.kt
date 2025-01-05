package com.flixclusive.domain.updater.util

import com.flixclusive.model.provider.ProviderMetadata

internal fun List<ProviderMetadata>.findProviderMetadata(
    providerName: String
): ProviderMetadata {
    return firstOrNull { it.name.equals(providerName, true) }
        ?: throw IllegalArgumentException("Provider not found: $providerName")
}