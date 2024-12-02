package com.flixclusive.feature.mobile.settings.screen.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.feature.mobile.settings.component.BaseSubScreen
import com.flixclusive.feature.mobile.settings.component.SettingsGroup
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalDialogKeyMap
import com.flixclusive.feature.mobile.settings.util.UiUtil.toggle
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun GeneralSettingsScreen(
    searchHistoryCount: Int,
    onClearSearchHistory: () -> Unit,
    onUsePrereleaseUpdatesChange: (Boolean) -> Unit
) {
    val dialogKeyMap = LocalDialogKeyMap.current
    val onItemClick = fun (item: SettingsItem) {
        when {
            item.onClick != null -> item.onClick.invoke()
            else -> dialogKeyMap.toggle(item.dialogKey!!)
        }
    }

    BaseSubScreen(
        title = stringResource(LocaleR.string.general),
        description = stringResource(LocaleR.string.general_settings_content_desc)
    ) {
        item {
            SettingsGroup(
                items = currentGeneralSettings(
                    searchHistoryCount = searchHistoryCount,
                    onUsePrereleaseUpdatesChange = onUsePrereleaseUpdatesChange
                ),
                onItemClick = onItemClick
            )
        }
    }

    GeneralDialogWrapper(
        onClearSearchHistory = onClearSearchHistory
    )
}