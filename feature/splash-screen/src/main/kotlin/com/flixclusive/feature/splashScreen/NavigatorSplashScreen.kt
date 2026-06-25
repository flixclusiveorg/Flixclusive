package com.flixclusive.feature.splashScreen

import com.flixclusive.core.navigation.navigator.NavigateToAddProfileScreen
import com.flixclusive.core.navigation.navigator.NavigateToAppUpdatesScreen
import com.flixclusive.core.navigation.navigator.NavigateToChooseProfileScreen
import com.flixclusive.core.navigation.navigator.NavigateToHomeScreen
import com.flixclusive.core.navigation.navigator.NavigateToOnboardingScreen
import com.flixclusive.core.navigation.navigator.NavigatorExitApp

interface NavigatorSplashScreen :
    NavigatorExitApp,
    NavigateToAppUpdatesScreen,
    NavigateToHomeScreen,
    NavigateToOnboardingScreen,
    NavigateToAddProfileScreen,
    NavigateToChooseProfileScreen
