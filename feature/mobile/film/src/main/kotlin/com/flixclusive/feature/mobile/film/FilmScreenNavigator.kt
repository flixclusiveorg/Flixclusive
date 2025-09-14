package com.flixclusive.feature.mobile.film

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.StartPlayerAction
import com.flixclusive.core.navigation.navigator.ViewFilmAction
import com.flixclusive.core.navigation.navigator.ViewFilmPreviewAction
import com.flixclusive.core.navigation.navigator.ViewGenreCatalogAction
import com.flixclusive.core.navigation.navigator.ViewProviderAction

interface FilmScreenNavigator :
    ViewFilmAction,
    ViewGenreCatalogAction,
    ViewFilmPreviewAction,
    ViewProviderAction,
    StartPlayerAction,
    GoBackAction
