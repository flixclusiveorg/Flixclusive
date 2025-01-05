package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.navargs.MarkdownNavigator
import com.flixclusive.model.provider.ProviderMetadata

interface ProvidersScreenNavigator : GoBackAction, ProviderTestNavigator, MarkdownNavigator {
    fun openProviderSettings(providerMetadata: ProviderMetadata)
    fun openProviderInfo(providerMetadata: ProviderMetadata)
    fun openAddRepositoryScreen()
}