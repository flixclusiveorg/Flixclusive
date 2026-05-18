package com.flixclusive.feature.mobile.provider.manage

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToAddProviderScreen
import com.flixclusive.core.navigation.navigator.NavigateToMarkdownScreen
import com.flixclusive.core.navigation.navigator.NavigateToProviderScreen
import com.flixclusive.core.navigation.navigator.NavigateToProviderSettings

interface NavigatorProviderManagerScreen :
    NavigateBack,
    NavigateToMarkdownScreen,
    NavigateToProviderScreen,
    NavigateToProviderSettings,
    NavigateToAddProviderScreen
