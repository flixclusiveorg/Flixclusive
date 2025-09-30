package com.flixclusive.feature.mobile.seeAll

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.ViewFilmAction
import com.flixclusive.core.navigation.navigator.ViewFilmPreviewAction

interface SeeAllScreenNavigator :
    GoBackAction,
    ViewFilmAction,
    ViewFilmPreviewAction
