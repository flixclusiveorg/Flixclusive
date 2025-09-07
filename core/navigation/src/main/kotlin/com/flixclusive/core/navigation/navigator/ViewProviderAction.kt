package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.provider.ProviderMetadata

interface ViewProviderAction {
    fun openProviderDetails(providerMetadata: ProviderMetadata)
}
