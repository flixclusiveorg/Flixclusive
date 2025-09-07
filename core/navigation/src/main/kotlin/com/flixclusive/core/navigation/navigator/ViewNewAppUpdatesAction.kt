package com.flixclusive.core.navigation.navigator

interface ViewNewAppUpdatesAction {
    fun openUpdateScreen(
        newVersion: String,
        updateUrl: String,
        updateInfo: String?,
        isComingFromSplashScreen: Boolean = false,
    )
}
