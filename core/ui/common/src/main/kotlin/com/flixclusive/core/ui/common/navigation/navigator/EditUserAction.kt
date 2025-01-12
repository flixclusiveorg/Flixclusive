package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.database.User

interface EditUserAction {
    fun openEditUserScreen(user: User)
}
