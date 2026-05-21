package com.flixclusive.feature.mobile.provider.details

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToAddProviderScreen
import com.flixclusive.core.navigation.navigator.NavigateToMarkdownScreen
import com.flixclusive.core.navigation.navigator.NavigateToProviderSettings

interface NavigatorProviderDetailsBottomSheet :
    NavigateBack,
    NavigateToAddProviderScreen,
    NavigateToProviderSettings,
    NavigateToMarkdownScreen
