package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.provider.ProviderMetadata

interface ViewProviderSettingsAction {
    fun openProviderSettings(providerMetadata: ProviderMetadata)
}
