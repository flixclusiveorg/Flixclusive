package com.flixclusive.feature.mobile.home

import com.flixclusive.core.navigation.navigator.AddProviderAction
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.StartPlayerAction
import com.flixclusive.core.navigation.navigator.ViewAllMediasAction
import com.flixclusive.core.navigation.navigator.ViewMediaAction
import com.flixclusive.core.navigation.navigator.ViewMediaPreviewAction

interface HomeNavigator :
    ViewMediaAction,
    ViewAllMediasAction,
    GoBackAction,
    ViewMediaPreviewAction,
    StartPlayerAction,
    AddProviderAction {
    fun openSearchScreen()
}
