package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.video_player

import androidx.compose.runtime.Composable
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_VIDEO_PLAYER_QUALITY_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.video_player.dialog.VideoPlayerQuality
import java.util.Locale

@Composable
fun VideoPlayerDialogWrapper(
    openedDialogMap: Map<String, Boolean>,
    appSettings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onDismissDialog: (String) -> Unit
) {
    when {
        //openedDialogMap[KEY_VIDEO_PLAYER_SERVER_DIALOG] == true -> {
        //    VideoPlayerServer(
        //        appSettings = appSettings,
        //        onChange = {
        //            onChange(appSettings.copy(preferredServer = it))
        //        },
        //        onDismissRequest = {
        //            onDismissDialog(KEY_VIDEO_PLAYER_SERVER_DIALOG)
        //        }
        //    )
        //}
        openedDialogMap[KEY_VIDEO_PLAYER_QUALITY_DIALOG] == true -> {
            VideoPlayerQuality(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(preferredQuality = it.lowercase(Locale.US)))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_VIDEO_PLAYER_QUALITY_DIALOG)
                }
            )
        }
    }
}