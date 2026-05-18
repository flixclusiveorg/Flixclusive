package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.provider.ProviderMetadata

interface NavigateToProviderScreen {
    fun navigateToProviderDetails(providerMetadata: ProviderMetadata)
}
