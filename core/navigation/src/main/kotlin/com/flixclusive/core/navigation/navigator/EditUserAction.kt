package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.database.entity.user.User

interface EditUserAction {
    fun openEditUserScreen(user: User)
}
