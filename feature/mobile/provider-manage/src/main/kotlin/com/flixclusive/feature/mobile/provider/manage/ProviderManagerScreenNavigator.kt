package com.flixclusive.feature.mobile.provider.manage

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.TestProvidersAction
import com.flixclusive.core.navigation.navigator.ViewMarkdownAction
import com.flixclusive.core.navigation.navigator.ViewProviderAction
import com.flixclusive.core.navigation.navigator.ViewProviderSettingsAction

interface ProviderManagerScreenNavigator :
    GoBackAction,
    TestProvidersAction,
    ViewMarkdownAction,
    ViewProviderAction,
    ViewProviderSettingsAction {
    fun openAddProviderScreen()
}
