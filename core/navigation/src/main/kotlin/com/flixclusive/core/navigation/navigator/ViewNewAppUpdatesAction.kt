package com.flixclusive.core.ui.common.navigation.navigator


interface ViewNewAppUpdatesAction {
    fun openUpdateScreen(
        newVersion: String,
        updateUrl: String,
        updateInfo: String?,
        isComingFromSplashScreen: Boolean = false
    )
}
