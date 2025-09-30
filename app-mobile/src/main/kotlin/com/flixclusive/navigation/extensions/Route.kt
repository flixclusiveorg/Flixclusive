package com.flixclusive.navigation.extensions

import com.flixclusive.feature.mobile.player.destinations.PlayerScreenDestination
import com.flixclusive.feature.splashScreen.destinations.SplashScreenDestination
import com.ramcosta.composedestinations.spec.Route

internal val Route.isSplashScreen: Boolean
    get() = this == SplashScreenDestination

internal val Route.isPlayerScreen: Boolean
    get() = this == PlayerScreenDestination
