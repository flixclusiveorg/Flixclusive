package com.flixclusive.core.ui.common.navigation.navargs

data class UpdateScreenNavArgs(
    val newVersion: String,
    val updateUrl: String,
    val updateInfo: String?,
    val isComingFromSplashScreen: Boolean
)