package com.flixclusive.feature.mobile.search

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.ViewAllFilmsAction

interface SearchScreenNavigator :
    GoBackAction,
    ViewAllFilmsAction {
    fun openSearchExpandedScreen()
}
