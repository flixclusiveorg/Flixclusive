package com.flixclusive.feature.mobile.media.navigator

import com.flixclusive.core.navigation.navigator.NavigateToLinkLoaderSheet
import com.flixclusive.core.navigation.navigator.NavigateToMediaImageDialog
import com.flixclusive.core.navigation.navigator.NavigateToMediaScreen

interface NavigatorMediaPreviewBottomSheet :
    NavigateToMediaScreen,
    NavigateToMediaImageDialog,
    NavigateToLinkLoaderSheet
