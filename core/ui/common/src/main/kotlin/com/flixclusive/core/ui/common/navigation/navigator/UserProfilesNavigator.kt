package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.StartHomeScreenAction
import com.flixclusive.model.database.User

interface UserProfilesNavigator : ExitNavigator, GoBackAction, StartHomeScreenAction {
    fun openEditUserScreen(user: User)
    fun openAddUserScreen()
}