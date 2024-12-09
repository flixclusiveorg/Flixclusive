package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.GoBackAction

interface SettingsScreenNavigator : GoBackAction {
    fun openProvidersScreen()

    fun openLink(url: String)
}