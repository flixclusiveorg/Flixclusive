package com.flixclusive.feature.mobile.media.navigator

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToLinkLoaderSheet
import com.flixclusive.core.navigation.navigator.NavigateToMediaPreviewBottomSheet
import com.flixclusive.core.navigation.navigator.NavigateToMediaScreen
import com.flixclusive.core.navigation.navigator.NavigateToProviderDetailsBottomSheet
import com.flixclusive.core.navigation.navigator.NavigateToSeeAllScreen

interface NavigatorMediaScreen :
    NavigateToMediaScreen,
    NavigateToMediaPreviewBottomSheet,
    NavigateToProviderDetailsBottomSheet,
    NavigateToLinkLoaderSheet,
    NavigateToSeeAllScreen,
    NavigateBack
