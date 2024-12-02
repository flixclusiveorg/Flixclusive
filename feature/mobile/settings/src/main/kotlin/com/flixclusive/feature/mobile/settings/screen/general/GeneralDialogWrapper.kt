package com.flixclusive.feature.mobile.settings.screen.general

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.ui.common.dialog.TextAlertDialog
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_SEARCH_HISTORY_NOTICE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalDialogKeyMap
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.rememberAppSettingsChanger
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.rememberLocalAppSettings
import com.flixclusive.feature.mobile.settings.util.UiUtil.toggle
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun GeneralDialogWrapper(
    onClearSearchHistory: () -> Unit,
    usePreReleaseUpdates: MutableState<Boolean>
) {
//    val context = LocalContext.current
    val appSettings by rememberLocalAppSettings()
    val onChangeSettings by rememberAppSettingsChanger()
    val dialogKeyMap = LocalDialogKeyMap.current

    when {
        dialogKeyMap[KEY_SEARCH_HISTORY_NOTICE_DIALOG] == true -> {
            TextAlertDialog(
                label = stringResource(LocaleR.string.clear_search_history),
                description = stringResource(LocaleR.string.clear_search_history_notice_msg),
                onConfirm = onClearSearchHistory,
                onDismiss = { dialogKeyMap.toggle(KEY_SEARCH_HISTORY_NOTICE_DIALOG) }
            )
        }
    }

    if (usePreReleaseUpdates.value) {
        PreReleaseUpdatesWarningDialog(
            onConfirm = {
                onChangeSettings(
                    appSettings.copy(isUsingPrereleaseUpdates = true)
                )
                usePreReleaseUpdates.value = false
            },
            onDismiss = { usePreReleaseUpdates.value = false }
        )
    }
}