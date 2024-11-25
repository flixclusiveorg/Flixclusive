package com.flixclusive.feature.mobile.settings.settings_group

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.KEY_DOH_DIALOG
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun currentNetworkSettings(
    onOpenUserAgentDialog: () -> Unit
): List<SettingsItem> {
    return listOf(
        SettingsItem(
            title = stringResource(LocaleR.string.doh),
            description = stringResource(LocaleR.string.dns_label),
            dialogKey = KEY_DOH_DIALOG,
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.default_user_agent),
            description = stringResource(LocaleR.string.default_user_agent_description),
            onClick = onOpenUserAgentDialog
        ),
    )
}