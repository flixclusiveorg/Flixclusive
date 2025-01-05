package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.provider.ProviderMetadata

interface ProviderInfoNavigator : RepositorySearchScreenNavigator, ProviderTestNavigator {
    fun seeWhatsNew(providerMetadata: ProviderMetadata)
    fun openProviderSettings(providerMetadata: ProviderMetadata)
}