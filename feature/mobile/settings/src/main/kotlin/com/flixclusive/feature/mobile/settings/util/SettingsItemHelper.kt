package com.flixclusive.feature.mobile.settings.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import com.flixclusive.model.datastore.AppSettings

internal val LocalAppSettings = compositionLocalOf { AppSettings() }
internal val LocalSettingsChanger = compositionLocalOf<((AppSettings) -> Unit)?> { null }

@Composable
internal fun rememberLocalAppSettings() = rememberUpdatedState(LocalAppSettings.current)

@Composable
internal fun rememberSettingsChanger(): State<((AppSettings) -> Unit)> {
    val localSettingsChanger = LocalSettingsChanger.current
    check(localSettingsChanger != null) {
        "LocalSettingsChanger not provided"
    }

    return rememberUpdatedState(localSettingsChanger)
}