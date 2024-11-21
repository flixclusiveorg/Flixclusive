package com.flixclusive.feature.mobile.settings.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import com.flixclusive.model.datastore.AppSettings

internal object AppSettingsHelper {
    val LocalAppSettings = compositionLocalOf { AppSettings() }
    val LocalAppSettingsChanger = compositionLocalOf<((AppSettings) -> Unit)?> { null }

    @Composable
    fun rememberLocalAppSettings() = rememberUpdatedState(LocalAppSettings.current)

    @Composable
    fun rememberAppSettingsChanger(): State<((AppSettings) -> Unit)> {
        val localSettingsChanger = LocalAppSettingsChanger.current
        check(localSettingsChanger != null) {
            "LocalAppSettingsChanger not provided"
        }

        return rememberUpdatedState(localSettingsChanger)
    }
}