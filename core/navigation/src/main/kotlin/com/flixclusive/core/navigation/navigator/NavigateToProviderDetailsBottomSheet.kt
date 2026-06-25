package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.provider.ProviderMetadata

interface NavigateToProviderDetailsBottomSheet {
    fun showProviderDetailsSheet(provider: ProviderMetadata)
}
