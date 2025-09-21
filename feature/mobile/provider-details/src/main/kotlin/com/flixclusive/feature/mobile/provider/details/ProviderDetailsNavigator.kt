package com.flixclusive.feature.mobile.provider.details

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.TestProvidersAction
import com.flixclusive.core.navigation.navigator.ViewMarkdownAction
import com.flixclusive.core.navigation.navigator.ViewProviderSettingsAction
import com.flixclusive.core.navigation.navigator.ViewRepositoryAction

interface ProviderDetailsNavigator :
    GoBackAction,
    ViewProviderSettingsAction,
    ViewRepositoryAction,
    TestProvidersAction,
    ViewMarkdownAction
