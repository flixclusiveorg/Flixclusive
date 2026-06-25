package com.flixclusive.domain.provider.usecase.manage

import com.flixclusive.data.provider.ProviderCapability

interface ToggleCapabilityUseCase {
    operator fun invoke(id: String, capability: ProviderCapability)

    operator fun invoke(id: String, capability: ProviderCapability, enabled: Boolean)
}
