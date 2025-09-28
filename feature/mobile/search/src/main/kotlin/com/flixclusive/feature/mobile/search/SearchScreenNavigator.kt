package com.flixclusive.feature.mobile.search

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.ViewAllFilmsAction
import com.flixclusive.core.navigation.navigator.ViewGenreCatalogAction

interface SearchScreenNavigator :
    GoBackAction,
    ViewGenreCatalogAction,
    ViewAllFilmsAction {
    fun openSearchExpandedScreen()
}
