package com.flixclusive.presentation.mobile.screens.preferences.settings

import androidx.compose.runtime.Composable

data class SettingsItem(
    val title: String,
    val description: String? = null,
    val enabled: Boolean = true,
    val dialogKey: String? = null,
    val previewContent: @Composable () -> Unit = {},
)