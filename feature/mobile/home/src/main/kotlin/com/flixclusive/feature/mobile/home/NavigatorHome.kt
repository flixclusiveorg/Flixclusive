package com.flixclusive.feature.mobile.home

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToAddProviderScreen
import com.flixclusive.core.navigation.navigator.NavigateToLinkLoaderSheet
import com.flixclusive.core.navigation.navigator.NavigateToMediaPreviewBottomSheet
import com.flixclusive.core.navigation.navigator.NavigateToMediaScreen
import com.flixclusive.core.navigation.navigator.NavigateToSeeAllScreen

interface NavigatorHome :
    NavigateToMediaScreen,
    NavigateToSeeAllScreen,
    NavigateBack,
    NavigateToMediaPreviewBottomSheet,
    NavigateToLinkLoaderSheet,
    NavigateToAddProviderScreen
