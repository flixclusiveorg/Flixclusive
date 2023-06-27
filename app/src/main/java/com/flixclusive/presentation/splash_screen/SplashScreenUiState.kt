package com.flixclusive.presentation.splash_screen

data class SplashScreenUiState(
    val isDoneInitializing: Boolean = false,
    val isNeedingAnUpdate: Boolean = false,
    val updateUrl: String = "",
    val isError: Boolean = false,
)