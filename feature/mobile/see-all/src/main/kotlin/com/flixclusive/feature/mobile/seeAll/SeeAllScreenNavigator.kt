package com.flixclusive.feature.mobile.seeAll

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.ViewMediaAction
import com.flixclusive.core.navigation.navigator.ViewMediaPreviewAction

interface SeeAllScreenNavigator :
    GoBackAction,
    ViewMediaAction,
    ViewMediaPreviewAction
