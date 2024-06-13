package com.flixclusive.feature.mobile.settings.settings_group

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.feature.mobile.settings.util.rememberLocalAppSettings
import com.flixclusive.feature.mobile.settings.util.rememberSettingsChanger
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun currentGeneralSettings(
    onUsePrereleaseUpdatesChange: (Boolean) -> Unit,
): List<SettingsItem> {
    val appSettings by rememberLocalAppSettings()
    val onChangeSettings by rememberSettingsChanger()

    return listOf(
        SettingsItem(
            title = stringResource(UtilR.string.notify_about_new_app_updates),
            onClick = {
                onChangeSettings(
                    appSettings.copy(
                        isUsingAutoUpdateAppFeature = !appSettings.isUsingAutoUpdateAppFeature
                    )
                )
            },
            previewContent = {
                Switch(
                    checked = appSettings.isUsingAutoUpdateAppFeature,
                    onCheckedChange = {
                        onChangeSettings(
                            appSettings.copy(
                                isUsingAutoUpdateAppFeature = !appSettings.isUsingAutoUpdateAppFeature
                            )
                        )
                    },
                    modifier = Modifier.scale(0.7F)
                )
            },
        ),
        SettingsItem(
            title = stringResource(UtilR.string.auto_update_providers),
            onClick = {
                onChangeSettings(
                    appSettings.copy(
                        isUsingAutoUpdateProviderFeature = !appSettings.isUsingAutoUpdateProviderFeature
                    )
                )
            },
            previewContent = {
                Switch(
                    checked = appSettings.isUsingAutoUpdateProviderFeature,
                    onCheckedChange = {
                        onChangeSettings(
                            appSettings.copy(
                                isUsingAutoUpdateProviderFeature = !appSettings.isUsingAutoUpdateProviderFeature
                            )
                        )
                    },
                    modifier = Modifier.scale(0.7F)
                )
            },
        ),
        SettingsItem(
            title = stringResource(UtilR.string.sign_up_prerelease),
            description = stringResource(UtilR.string.signup_prerelease_updates_desc),
            onClick = {
                if (!appSettings.isUsingPrereleaseUpdates) {
                    onChangeSettings(appSettings.copy(isUsingPrereleaseUpdates = false))
                } else onUsePrereleaseUpdatesChange(true)
            },
            previewContent = {
                Switch(
                    checked = appSettings.isUsingPrereleaseUpdates,
                    onCheckedChange = {
                        if (!it) {
                            onChangeSettings(appSettings.copy(isUsingPrereleaseUpdates = false))
                        } else {
                            onUsePrereleaseUpdatesChange(true)
                        }
                    },
                    modifier = Modifier.scale(0.7F)
                )
            },
        ),
        SettingsItem(
            title = stringResource(UtilR.string.automatic_crash_report),
            description = stringResource(UtilR.string.automatic_crash_report_label),
            onClick = {
                onChangeSettings(appSettings.copy(isSendingCrashLogsAutomatically = !appSettings.isSendingCrashLogsAutomatically))
            },
            previewContent = {
                Switch(
                    checked = appSettings.isSendingCrashLogsAutomatically,
                    onCheckedChange = {
                        onChangeSettings(appSettings.copy(isSendingCrashLogsAutomatically = it))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            },
        ),
    )
}