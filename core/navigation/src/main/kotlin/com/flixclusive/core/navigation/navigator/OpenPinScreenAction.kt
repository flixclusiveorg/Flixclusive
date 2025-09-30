package com.flixclusive.core.navigation.navigator

import java.io.Serializable

sealed class PinAction : Serializable {
    data class Verify(val userPin: String) : PinAction()

    object Setup : PinAction() {
        private fun readResolve(): Any = Setup
    }
}

interface OpenPinScreenAction {
    fun openUserPinScreen(action: PinAction)
}
