package com.flixclusive.feature.mobile.provider.details

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToAddProviderScreen
import com.flixclusive.core.navigation.navigator.NavigateToMarkdownScreen
import com.flixclusive.core.navigation.navigator.NavigateToProviderSettingsScreen

interface NavigatorProviderDetailsBottomSheet :
    NavigateBack,
    NavigateToAddProviderScreen,
    NavigateToProviderSettingsScreen,
    NavigateToMarkdownScreen
