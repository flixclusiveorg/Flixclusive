package com.flixclusive.feature.mobile.search

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.ViewFilmAction
import com.flixclusive.core.navigation.navigator.ViewFilmPreviewAction

interface SearchScreenNavigator :
    GoBackAction,
    ViewFilmAction,
    ViewFilmPreviewAction
