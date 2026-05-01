package com.flixclusive.feature.mobile.media

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.StartPlayerAction
import com.flixclusive.core.navigation.navigator.ViewAllMediasAction
import com.flixclusive.core.navigation.navigator.ViewMediaAction
import com.flixclusive.core.navigation.navigator.ViewMediaPreviewAction
import com.flixclusive.core.navigation.navigator.ViewProviderAction

interface MediaScreenNavigator :
    ViewMediaAction,
    ViewMediaPreviewAction,
    ViewProviderAction,
    StartPlayerAction,
    ViewAllMediasAction,
    GoBackAction
