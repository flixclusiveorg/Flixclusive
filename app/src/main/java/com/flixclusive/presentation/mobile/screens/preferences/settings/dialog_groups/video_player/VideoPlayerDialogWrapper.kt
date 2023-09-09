package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.video_player

import androidx.compose.runtime.Composable
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_VIDEO_PLAYER_SERVER_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.video_player.dialog.VideoPlayerServer

@Composable
fun VideoPlayerDialogWrapper(
    openedDialogMap: Map<String, Boolean>,
    appSettings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onDismissDialog: (String) -> Unit
) {
    when {
        openedDialogMap[KEY_VIDEO_PLAYER_SERVER_DIALOG] == true -> {
            VideoPlayerServer(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(preferredServer = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_VIDEO_PLAYER_SERVER_DIALOG)
                }
            )
        }
    }
}