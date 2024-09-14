package com.flixclusive.domain.updater.util

import com.flixclusive.model.provider.ProviderData

internal fun List<ProviderData>.findProviderData(
    providerName: String
): ProviderData {
    return firstOrNull { it.name.equals(providerName, true) }
        ?: throw IllegalArgumentException("Provider not found: $providerName")
}