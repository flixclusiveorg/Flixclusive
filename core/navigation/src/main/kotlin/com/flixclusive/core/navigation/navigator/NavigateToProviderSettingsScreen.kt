package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.provider.ProviderMetadata

interface NavigateToProviderSettingsScreen {
    fun navigateToProviderSettings(provider: ProviderMetadata)
}
