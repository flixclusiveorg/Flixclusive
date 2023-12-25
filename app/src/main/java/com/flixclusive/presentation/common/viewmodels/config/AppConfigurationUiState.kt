package com.flixclusive.presentation.common.viewmodels.config

import com.flixclusive.common.UiText

data class AppConfigurationUiState(
    val isDoneInitializing: Boolean = false,
    val isNeedingAnUpdate: Boolean = false,
    val updateUrl: String = "",
    val newVersion: String = "",
    val updateInfo: String? = null,
    val errorMessage: UiText? = null,
    val isMaintenance: Boolean = false,
    val isShowingTvScreen: Boolean = false,
    val allPermissionsAreAllowed: Boolean = false,
)