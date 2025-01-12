package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.provider.ProviderMetadata

interface ViewProviderSettingsAction {
    fun openProviderSettings(providerMetadata: ProviderMetadata)
}
