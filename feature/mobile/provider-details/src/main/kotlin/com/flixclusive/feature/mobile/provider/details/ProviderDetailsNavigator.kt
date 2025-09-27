package com.flixclusive.feature.mobile.provider.details

import com.flixclusive.core.navigation.navigator.AddProviderAction
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.TestProvidersAction
import com.flixclusive.core.navigation.navigator.ViewMarkdownAction
import com.flixclusive.core.navigation.navigator.ViewProviderSettingsAction

interface ProviderDetailsNavigator :
    GoBackAction,
    AddProviderAction,
    ViewProviderSettingsAction,
    TestProvidersAction,
    ViewMarkdownAction
