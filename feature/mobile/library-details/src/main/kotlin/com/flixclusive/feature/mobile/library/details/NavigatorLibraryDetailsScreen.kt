package com.flixclusive.feature.mobile.library.details

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToMediaPreviewBottomSheet
import com.flixclusive.core.navigation.navigator.NavigateToMediaScreen

interface NavigatorLibraryDetailsScreen :
    NavigateToMediaScreen,
    NavigateToMediaPreviewBottomSheet,
    NavigateBack
