package com.flixclusive.feature.mobile.provider.manage

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToAddProviderScreen
import com.flixclusive.core.navigation.navigator.NavigateToMarkdownScreen
import com.flixclusive.core.navigation.navigator.NavigateToProviderDetailsBottomSheet
import com.flixclusive.core.navigation.navigator.NavigateToProviderSettingsScreen

interface NavigatorProviderManagerScreen :
    NavigateBack,
    NavigateToMarkdownScreen,
    NavigateToProviderDetailsBottomSheet,
    NavigateToProviderSettingsScreen,
    NavigateToAddProviderScreen
