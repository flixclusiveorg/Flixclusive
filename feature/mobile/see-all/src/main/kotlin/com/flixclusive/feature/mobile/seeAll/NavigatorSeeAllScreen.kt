package com.flixclusive.feature.mobile.seeAll

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToMediaPreviewBottomSheet
import com.flixclusive.core.navigation.navigator.NavigateToMediaScreen

interface NavigatorSeeAllScreen :
    NavigateBack,
    NavigateToMediaScreen,
    NavigateToMediaPreviewBottomSheet
