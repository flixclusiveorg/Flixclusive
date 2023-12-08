package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.advanced

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_DOH_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.advanced.dialog.AdvancedDialogDoH
import com.flixclusive.presentation.utils.showToast

@Composable
fun AdvancedDialogWrapper(
    openedDialogMap: Map<String, Boolean>,
    appSettings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onDismissDialog: (String) -> Unit
) {
    val context = LocalContext.current

    when {
        openedDialogMap[KEY_DOH_DIALOG] == true -> {
            AdvancedDialogDoH(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(dns = it))
                    onDismissDialog(KEY_DOH_DIALOG)
                    context.showToast("Restart is required for this to take effect.")
                },
                onDismissRequest = {
                    onDismissDialog(KEY_DOH_DIALOG)
                }
            )
        }
    }
}