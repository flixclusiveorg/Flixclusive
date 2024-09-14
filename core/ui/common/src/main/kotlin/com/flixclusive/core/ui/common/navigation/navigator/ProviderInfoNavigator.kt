package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.provider.ProviderData

interface ProviderInfoNavigator : RepositorySearchScreenNavigator, ProviderTestNavigator {
    fun seeWhatsNew(providerData: ProviderData)
    fun openProviderSettings(providerData: ProviderData)
}