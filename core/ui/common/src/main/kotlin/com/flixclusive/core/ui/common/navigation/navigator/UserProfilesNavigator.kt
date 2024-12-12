package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.StartHomeScreenAction

interface UserProfilesNavigator : ExitNavigator, GoBackAction, StartHomeScreenAction {
    fun openEditUserScreen()
    fun openAddUsersScreen()
}