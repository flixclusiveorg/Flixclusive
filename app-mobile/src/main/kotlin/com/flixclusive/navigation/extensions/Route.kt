package com.flixclusive.navigation.extensions

import com.ramcosta.composedestinations.generated.player.destinations.PlayerScreenDestination
import com.ramcosta.composedestinations.generated.splashscreen.destinations.SplashScreenDestination
import com.ramcosta.composedestinations.spec.Route

internal val Route.isSplashScreen: Boolean
    get() = this == SplashScreenDestination

internal val Route.isPlayerScreen: Boolean
    get() = this == PlayerScreenDestination
