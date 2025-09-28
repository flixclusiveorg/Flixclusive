package com.flixclusive.feature.mobile.settings.screen.root

import com.flixclusive.core.navigation.navigator.ChooseProfileAction
import com.flixclusive.core.navigation.navigator.EditUserAction
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.TestProvidersAction

interface SettingsScreenNavigator :
    GoBackAction,
    ChooseProfileAction,
    TestProvidersAction,
    EditUserAction {
    fun openRepositoryManagerScreen()

    fun openProviderManagerScreen()

    fun openLink(url: String)
}
