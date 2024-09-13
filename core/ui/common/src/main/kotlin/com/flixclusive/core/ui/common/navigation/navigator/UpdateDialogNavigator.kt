package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.core.ui.common.navigation.GoBackAction


interface UpdateDialogNavigator : GoBackAction {
    fun openUpdateScreen(
        newVersion: String,
        updateUrl: String,
        updateInfo: String?,
        isComingFromSplashScreen: Boolean = false
    )
}