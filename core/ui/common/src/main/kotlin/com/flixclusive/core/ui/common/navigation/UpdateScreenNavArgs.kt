package com.flixclusive.core.ui.common.navigation


interface UpdateDialogNavigator : GoBackAction {
    fun openUpdateScreen(
        newVersion: String,
        updateUrl: String,
        updateInfo: String?,
    )
}