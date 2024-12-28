package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.StartHomeScreenAction

interface CommonUserEditNavigator : GoBackAction, StartHomeScreenAction {
    fun openUserAvatarSelectScreen(selected: Int)
    fun openUserPinSetupScreen(
        currentPin: String? = null,
        isRemovingPin: Boolean = false
    )
}