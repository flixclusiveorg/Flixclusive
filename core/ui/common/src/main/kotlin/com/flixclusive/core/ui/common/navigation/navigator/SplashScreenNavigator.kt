package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.StartHomeScreenAction

interface SplashScreenNavigator : UpdateDialogNavigator, StartHomeScreenAction {
    fun onExitApplication()
}