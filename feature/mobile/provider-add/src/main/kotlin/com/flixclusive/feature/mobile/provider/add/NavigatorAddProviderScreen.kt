package com.flixclusive.feature.mobile.provider.add

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToProviderDetailsBottomSheet
import com.flixclusive.core.navigation.navigator.NavigateToProviderSettingsScreen

interface NavigatorAddProviderScreen :
    NavigateToProviderDetailsBottomSheet,
    NavigateToProviderSettingsScreen,
    NavigateBack
