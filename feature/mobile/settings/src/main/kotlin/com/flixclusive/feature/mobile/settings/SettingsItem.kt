package com.flixclusive.feature.mobile.settings

import androidx.compose.runtime.Composable

internal data class SettingsItem(
    val title: String,
    val description: String? = null,
    val enabled: Boolean = true,
    val dialogKey: String? = null,
    val onClick: (() -> Unit)? = null,
    val previewContent: @Composable () -> Unit = {},
)