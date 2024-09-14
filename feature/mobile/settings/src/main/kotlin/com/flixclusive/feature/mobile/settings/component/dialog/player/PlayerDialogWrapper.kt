package com.flixclusive.feature.mobile.settings.component.dialog.player

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.feature.mobile.settings.KEY_AUDIO_LANGUAGE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_DECODER_PRIORITY_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_BUFFER_LENGTH_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_BUFFER_SIZE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_DISK_CACHE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_QUALITY_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_RESIZE_MODE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG
import com.flixclusive.feature.mobile.settings.component.dialog.CommonSettingsDialog
import com.flixclusive.feature.mobile.settings.component.dialog.LanguageDialog
import com.flixclusive.feature.mobile.settings.component.dialog.player.Constant.availableSeekIncrementMs
import com.flixclusive.feature.mobile.settings.component.dialog.player.Constant.playerBufferLengths
import com.flixclusive.feature.mobile.settings.component.dialog.player.Constant.playerBufferSizes
import com.flixclusive.feature.mobile.settings.component.dialog.player.Constant.playerCacheSizes
import com.flixclusive.feature.mobile.settings.util.AppSettingsHelper.rememberLocalAppSettings
import com.flixclusive.feature.mobile.settings.util.AppSettingsHelper.rememberAppSettingsChanger
import com.flixclusive.model.datastore.DEFAULT_PLAYER_BUFFER_AMOUNT
import com.flixclusive.model.datastore.DEFAULT_PLAYER_CACHE_SIZE_AMOUNT
import com.flixclusive.model.datastore.player.DecoderPriority
import com.flixclusive.model.datastore.player.PlayerQuality
import com.flixclusive.model.datastore.player.ResizeMode
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun PlayerDialogWrapper(
    openedDialogMap: Map<String, Boolean>,
    onDismissDialog: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val appSettings by rememberLocalAppSettings()
    val onChangeSettings by rememberAppSettingsChanger()

    when {
        openedDialogMap[KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG] == true -> {
            CommonSettingsDialog(
                label = stringResource(id = LocaleR.string.preferred_quality),
                options = availableSeekIncrementMs,
                selectedOption = remember { mutableStateOf(appSettings.preferredSeekAmount) },
                optionLabelExtractor = {
                    stringResource(LocaleR.string.seek_seconds_format, it.div(1000))
                },
                onChange = {
                    onChangeSettings(appSettings.copy(preferredSeekAmount = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_PLAYER_QUALITY_DIALOG] == true -> {
            CommonSettingsDialog(
                label = stringResource(id = LocaleR.string.preferred_quality),
                options = PlayerQuality.entries,
                selectedOption = remember { mutableStateOf(appSettings.preferredQuality) },
                optionLabelExtractor = { it.qualityName.asString() },
                onChange = {
                    onChangeSettings(appSettings.copy(preferredQuality = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_PLAYER_QUALITY_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_PLAYER_RESIZE_MODE_DIALOG] == true -> {
            CommonSettingsDialog(
                label = stringResource(id = LocaleR.string.preferred_resize_mode),
                options = remember { ResizeMode.entries.map { it.ordinal } },
                selectedOption = remember { mutableStateOf(appSettings.preferredResizeMode) },
                optionLabelExtractor = { ResizeMode.entries[it].toString() },
                onChange = {
                    onChangeSettings(appSettings.copy(preferredResizeMode = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_PLAYER_RESIZE_MODE_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_PLAYER_DISK_CACHE_DIALOG] == true -> {
            CommonSettingsDialog(
                label = stringResource(id = LocaleR.string.video_cache_size),
                options = playerCacheSizes,
                selectedOption = remember { mutableStateOf(appSettings.preferredDiskCacheSize) },
                optionLabelExtractor = {
                    when (it) {
                        0L -> context.getString(LocaleR.string.none_label)
                        -1L -> context.getString(LocaleR.string.no_cache_limit_label)
                        else -> Formatter.formatShortFileSize(
                            /* context = */ context,
                            /* sizeBytes = */ it * 1000L * 1000L
                        ) + if(it == DEFAULT_PLAYER_CACHE_SIZE_AMOUNT) " " + context.getString(LocaleR.string.default_label) else ""
                    }
                },
                onChange = {
                    val isTheSameItem = it == appSettings.preferredDiskCacheSize

                    if(!isTheSameItem) {
                        // If cache is set to `None`, then clear the current cache
                        if(it == 0L) {
                            safeCall {
                                scope.launch { context.cacheDir.deleteRecursively() }
                            }
                        }

                        onChangeSettings(appSettings.copy(preferredDiskCacheSize = it))
                    }
                },
                onDismissRequest = {
                    onDismissDialog(KEY_PLAYER_DISK_CACHE_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_PLAYER_BUFFER_SIZE_DIALOG] == true -> {
            CommonSettingsDialog(
                label = stringResource(id = LocaleR.string.video_buffer_size),
                options = playerBufferSizes,
                selectedOption = remember { mutableStateOf(appSettings.preferredBufferCacheSize) },
                optionLabelExtractor = {
                    if (it == -1L)
                        context.getString(LocaleR.string.auto_option)
                    else Formatter.formatShortFileSize(
                        /* context = */ context,
                        /* sizeBytes = */ it * 1000L * 1000L
                    )
                },
                onChange = {
                    onChangeSettings(appSettings.copy(preferredBufferCacheSize = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_PLAYER_BUFFER_SIZE_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_PLAYER_BUFFER_LENGTH_DIALOG] == true -> {
            CommonSettingsDialog(
                label = stringResource(id = LocaleR.string.video_buffer_max_length),
                options = playerBufferLengths.keys.toList(),
                selectedOption = remember { mutableStateOf(appSettings.preferredVideoBufferMs) },
                optionLabelExtractor = {
                    playerBufferLengths[it] + when (it) {
                        DEFAULT_PLAYER_BUFFER_AMOUNT -> context.getString(LocaleR.string.default_label)
                        else -> ""
                    }
                },
                onChange = {
                    onChangeSettings(appSettings.copy(preferredVideoBufferMs = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_PLAYER_BUFFER_LENGTH_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_AUDIO_LANGUAGE_DIALOG] == true -> {
            LanguageDialog(
                appSettings = appSettings,
                selectedOption = remember { mutableStateOf(appSettings.preferredAudioLanguage) },
                label = stringResource(id = LocaleR.string.preferred_audio_language),
                onChange = {
                    onChangeSettings(appSettings.copy(preferredAudioLanguage = it.language))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_AUDIO_LANGUAGE_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_DECODER_PRIORITY_DIALOG] == true -> {
            CommonSettingsDialog(
                label = stringResource(id = LocaleR.string.decoder_priority),
                options = DecoderPriority.entries,
                selectedOption = remember { mutableStateOf(appSettings.decoderPriority) },
                optionLabelExtractor = { it.toUiText().asString() },
                onChange = {
                    onChangeSettings(appSettings.copy(decoderPriority = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_DECODER_PRIORITY_DIALOG)
                }
            )
        }
    }
}