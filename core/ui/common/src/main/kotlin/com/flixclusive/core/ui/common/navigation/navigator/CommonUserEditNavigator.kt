package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.GoBackAction

interface CommonUserEditNavigator : GoBackAction {
    fun openUserAvatarSelectScreen(selected: Int)
}