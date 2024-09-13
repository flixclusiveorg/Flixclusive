package com.flixclusive.core.ui.common.navigation.navigator

interface PreferencesScreenNavigator {
    fun openWatchlistScreen()
    fun openRecentlyWatchedScreen()
    fun openSettingsScreen()
    fun openAboutScreen()
    fun checkForUpdates()
}