package com.flixclusive.feature.mobile.home

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.StartPlayerAction
import com.flixclusive.core.navigation.navigator.ViewAllFilmsAction
import com.flixclusive.core.navigation.navigator.ViewFilmAction
import com.flixclusive.core.navigation.navigator.ViewFilmPreviewAction

interface HomeNavigator :
    ViewFilmAction,
    ViewAllFilmsAction,
    GoBackAction,
    ViewFilmPreviewAction,
    StartPlayerAction
