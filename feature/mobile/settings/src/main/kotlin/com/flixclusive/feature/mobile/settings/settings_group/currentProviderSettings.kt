package com.flixclusive.feature.mobile.settings.settings_group

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.feature.mobile.settings.util.ProviderSettingsHelper.rememberAppSettingsProviderChanger
import com.flixclusive.feature.mobile.settings.util.ProviderSettingsHelper.rememberLocalAppSettingsProvider
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun currentProviderSettings(): List<SettingsItem> {
    val appSettingsProvider by rememberLocalAppSettingsProvider()
    val onChangeSettings by rememberAppSettingsProviderChanger()

    return listOf(
        SettingsItem(
            title = stringResource(LocaleR.string.auto_update_providers),
            onClick = {
                onChangeSettings(
                    appSettingsProvider.copy(
                        isUsingAutoUpdateProviderFeature = !appSettingsProvider.isUsingAutoUpdateProviderFeature
                    )
                )
            },
            previewContent = {
                Switch(
                    checked = appSettingsProvider.isUsingAutoUpdateProviderFeature,
                    onCheckedChange = {
                        onChangeSettings(
                            appSettingsProvider.copy(
                                isUsingAutoUpdateProviderFeature = !appSettingsProvider.isUsingAutoUpdateProviderFeature
                            )
                        )
                    },
                    modifier = Modifier.scale(0.7F)
                )
            },
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.warn_on_unsafe_install),
            description = stringResource(LocaleR.string.warn_on_unsafe_install_description),
            onClick = {
                onChangeSettings(
                    appSettingsProvider.copy(
                        warnOnInstall = !appSettingsProvider.warnOnInstall
                    )
                )
            },
            previewContent = {
                Switch(
                    checked = appSettingsProvider.warnOnInstall,
                    onCheckedChange = {
                        onChangeSettings(
                            appSettingsProvider.copy(
                                warnOnInstall = !appSettingsProvider.warnOnInstall
                            )
                        )
                    },
                    modifier = Modifier.scale(0.7F)
                )
            },
        )
    )
}