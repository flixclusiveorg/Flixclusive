package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.database.entity.user.User
import java.io.Serializable

sealed class PinAction : Serializable {
    data class Verify(
        val user: User,
    ) : PinAction()

    object Setup : PinAction() {
        private fun readResolve(): Any = Setup
    }
}

interface OpenPinScreenAction {
    fun openUserPinScreen(action: PinAction)
}
