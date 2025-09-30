package com.flixclusive.feature.mobile.searchExpanded

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.ViewFilmAction
import com.flixclusive.core.navigation.navigator.ViewFilmPreviewAction

interface SearchExpandedScreenNavigator :
    GoBackAction,
    ViewFilmAction,
    ViewFilmPreviewAction
