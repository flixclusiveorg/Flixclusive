package com.flixclusive.feature.mobile.settings.screen.root

import com.flixclusive.core.navigation.navigator.ChooseProfileAction
import com.flixclusive.core.navigation.navigator.EditUserAction
import com.flixclusive.core.navigation.navigator.GoBackAction

interface SettingsScreenNavigator :
    GoBackAction,
    ChooseProfileAction,
    EditUserAction {
    fun openRepositoryManagerScreen()

    fun openProviderManagerScreen()

    fun openLink(url: String)
}
