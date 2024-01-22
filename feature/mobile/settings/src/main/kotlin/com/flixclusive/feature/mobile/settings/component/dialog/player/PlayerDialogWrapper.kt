package com.flixclusive.feature.mobile.settings.component.dialog.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_BUFFER_LENGTH_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_BUFFER_SIZE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_DISK_CACHE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_QUALITY_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_RESIZE_MODE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG
import com.flixclusive.feature.mobile.settings.component.dialog.player.dialog.PlayerBufferLength
import com.flixclusive.feature.mobile.settings.component.dialog.player.dialog.PlayerBufferSize
import com.flixclusive.feature.mobile.settings.component.dialog.player.dialog.PlayerDiskCacheSize
import com.flixclusive.feature.mobile.settings.component.dialog.player.dialog.PlayerQuality
import com.flixclusive.feature.mobile.settings.component.dialog.player.dialog.PlayerResizeMode
import com.flixclusive.feature.mobile.settings.component.dialog.player.dialog.PlayerSeekLength
import com.flixclusive.model.datastore.AppSettings
import kotlinx.coroutines.launch

@Composable
internal fun PlayerDialogWrapper(
    openedDialogMap: Map<String, Boolean>,
    appSettings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onDismissDialog: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                    onChange(appSettings.copy(preferredQuality = it))
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
                    val isTheSameItem = it == appSettings.preferredDiskCacheSize

                    // If cache is set to `None`, then clear the current cache

                    if(!isTheSameItem) {
                        if(it == 0L) {
                            safeCall {
                                scope.launch { context.cacheDir.deleteRecursively() }
                            }
                        }

                        onChange(
                            appSettings.copy(
                                preferredDiskCacheSize = it,
                                shouldNotifyAboutCache = true
                            )
                        )
                    }
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