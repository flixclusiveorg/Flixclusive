package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.player

import androidx.compose.runtime.Composable
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_PLAYER_BUFFER_LENGTH_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_PLAYER_BUFFER_SIZE_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_PLAYER_DISK_CACHE_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_PLAYER_QUALITY_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_PLAYER_RESIZE_MODE_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.player.dialog.PlayerBufferLength
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.player.dialog.PlayerBufferSize
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.player.dialog.PlayerDiskCacheSize
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.player.dialog.PlayerQuality
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.player.dialog.PlayerResizeMode
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.player.dialog.PlayerSeekLength
import java.util.Locale

@Composable
fun PlayerDialogWrapper(
    openedDialogMap: Map<String, Boolean>,
    appSettings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onDismissDialog: (String) -> Unit
) {
    when {
        openedDialogMap[KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG] == true -> {
            PlayerSeekLength(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(preferredSeekAmount = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_PLAYER_QUALITY_DIALOG] == true -> {
            PlayerQuality(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(preferredQuality = it.lowercase(Locale.US)))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_PLAYER_QUALITY_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_PLAYER_RESIZE_MODE_DIALOG] == true -> {
            PlayerResizeMode(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(preferredResizeMode = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_PLAYER_RESIZE_MODE_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_PLAYER_DISK_CACHE_DIALOG] == true -> {
            PlayerDiskCacheSize(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(preferredDiskCacheSize = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_PLAYER_DISK_CACHE_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_PLAYER_BUFFER_SIZE_DIALOG] == true -> {
            PlayerBufferSize(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(preferredBufferCacheSize = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_PLAYER_BUFFER_SIZE_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_PLAYER_BUFFER_LENGTH_DIALOG] == true -> {
            PlayerBufferLength(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(preferredVideoBufferMs = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_PLAYER_BUFFER_LENGTH_DIALOG)
                }
            )
        }
    }
}