package com.flixclusive.feature.mobile.settings.screen.player.items

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.rememberAppSettingsChanger
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.rememberLocalAppSettings
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun playerUiItems(): List<SettingsItem> {
    val appSettings by rememberLocalAppSettings()
    val onChangeSettings by rememberAppSettingsChanger()

    return listOf(
        SettingsItem(
            title = stringResource(LocaleR.string.reverse_player_time),
            onClick = {
                onChangeSettings(appSettings.copy(isPlayerTimeReversed = !appSettings.isPlayerTimeReversed))
            },
            content = {
                Switch(
                    checked = appSettings.isPlayerTimeReversed,
                    onCheckedChange = {
                        onChangeSettings(appSettings.copy(isPlayerTimeReversed = !appSettings.isPlayerTimeReversed))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            }
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.pip_mode),
            description = stringResource(LocaleR.string.pip_mode_desc),
            onClick = {
                onChangeSettings(appSettings.copy(isPiPModeEnabled = !appSettings.isPiPModeEnabled))
            },
            content = {
                Switch(
                    checked = appSettings.isPiPModeEnabled,
                    onCheckedChange = {
                        onChangeSettings(appSettings.copy(isPiPModeEnabled = !appSettings.isPiPModeEnabled))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            }
        )
    )
}