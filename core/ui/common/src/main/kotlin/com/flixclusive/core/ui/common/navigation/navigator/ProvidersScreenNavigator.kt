package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.navargs.MarkdownNavigator
import com.flixclusive.gradle.entities.ProviderData

interface ProvidersScreenNavigator : GoBackAction, ProviderTestNavigator, MarkdownNavigator {
    fun openProviderSettings(providerData: ProviderData)
    fun openProviderInfo(providerData: ProviderData)
    fun openAddRepositoryScreen()
}