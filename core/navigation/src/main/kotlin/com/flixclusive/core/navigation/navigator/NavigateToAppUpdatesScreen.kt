package com.flixclusive.core.navigation.navigator

interface NavigateToAppUpdatesScreen {
    fun navigateToAppUpdateScreen(
        newVersion: String,
        updateUrl: String,
        updateInfo: String?,
        isComingFromSplashScreen: Boolean = false,
    )
}
