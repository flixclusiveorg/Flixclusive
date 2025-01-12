package com.flixclusive.core.ui.common.navigation.navigator

interface SetupPinAction {
    fun openUserPinSetupScreen(
        currentPin: String? = null,
        isRemovingPin: Boolean = false
    )
}
