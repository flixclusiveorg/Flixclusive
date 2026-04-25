package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.provider.ProviderPlugin

interface GetProviderPluginUseCase {
    suspend operator fun invoke(id: String): ProviderPlugin?
}
