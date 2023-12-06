package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles

import androidx.compose.runtime.Composable
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_SUBTITLE_COLOR_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_SUBTITLE_EDGE_TYPE_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_SUBTITLE_FONT_STYLE_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_SUBTITLE_LANGUAGE_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.KEY_SUBTITLE_SIZE_DIALOG
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles.dialog.SubtitleDialogEdgeType
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles.dialog.SubtitleDialogFontBackgroundColor
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles.dialog.SubtitleDialogFontColor
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles.dialog.SubtitleDialogFontStyle
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles.dialog.SubtitleDialogLanguages
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles.dialog.SubtitleDialogSize

@Composable
fun SubtitleDialogWrapper(
    openedDialogMap: Map<String, Boolean>,
    appSettings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onDismissDialog: (String) -> Unit
) {
    when {
        openedDialogMap[KEY_SUBTITLE_SIZE_DIALOG] == true -> {
            SubtitleDialogSize(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(subtitleSize = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_SUBTITLE_SIZE_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_SUBTITLE_LANGUAGE_DIALOG] == true -> {
            SubtitleDialogLanguages(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(subtitleLanguage = it.language))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_SUBTITLE_LANGUAGE_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_SUBTITLE_EDGE_TYPE_DIALOG] == true -> {
            SubtitleDialogEdgeType(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(subtitleEdgeType = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_SUBTITLE_EDGE_TYPE_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_SUBTITLE_FONT_STYLE_DIALOG] == true -> {
            SubtitleDialogFontStyle(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(subtitleFontStyle = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_SUBTITLE_FONT_STYLE_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_SUBTITLE_COLOR_DIALOG] == true -> {
            SubtitleDialogFontColor(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(subtitleColor = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_SUBTITLE_COLOR_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG] == true -> {
            SubtitleDialogFontBackgroundColor(
                appSettings = appSettings,
                onChange = {
                    onChange(appSettings.copy(subtitleBackgroundColor = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG)
                }
            )
        }
    }
}