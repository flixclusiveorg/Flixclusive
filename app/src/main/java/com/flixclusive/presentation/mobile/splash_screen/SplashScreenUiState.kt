package com.flixclusive.presentation.mobile.splash_screen

data class SplashScreenUiState(
    val isDoneInitializing: Boolean = false,
    val isNeedingAnUpdate: Boolean = false,
    val updateUrl: String = "",
    val isError: Boolean = false,
    val isMaintenance: Boolean = false,
)