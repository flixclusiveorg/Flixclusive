package com.flixclusive.feature.mobile.settings.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import com.flixclusive.model.datastore.AppSettingsProvider

internal object ProviderSettingsHelper {
    val LocalAppSettingsProvider = compositionLocalOf { AppSettingsProvider() }
    val LocalAppSettingsProviderChanger = compositionLocalOf<((AppSettingsProvider) -> Unit)?> { null }

    @Composable
    fun rememberLocalAppSettingsProvider() = rememberUpdatedState(LocalAppSettingsProvider.current)

    @Composable
    fun rememberAppSettingsProviderChanger(): State<((AppSettingsProvider) -> Unit)> {
        val localSettingsChanger = LocalAppSettingsProviderChanger.current
        check(localSettingsChanger != null) {
            "LocalSettingsChanger not provided"
        }

        return rememberUpdatedState(localSettingsChanger)
    }
}