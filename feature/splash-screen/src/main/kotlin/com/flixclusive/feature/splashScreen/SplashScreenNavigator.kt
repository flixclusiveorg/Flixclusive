package com.flixclusive.feature.splashScreen

import com.flixclusive.core.navigation.navigator.AddProfileAction
import com.flixclusive.core.navigation.navigator.ChooseProfileAction
import com.flixclusive.core.navigation.navigator.ExitAction
import com.flixclusive.core.navigation.navigator.StartHomeScreenAction
import com.flixclusive.core.navigation.navigator.ViewNewAppUpdatesAction

interface SplashScreenNavigator :
    ExitAction,
    ViewNewAppUpdatesAction,
    StartHomeScreenAction,
    AddProfileAction,
    ChooseProfileAction
