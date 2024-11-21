package com.flixclusive.feature.mobile.settings.component.dialog.subtitles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_SUBTITLE_COLOR_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_SUBTITLE_EDGE_TYPE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_SUBTITLE_FONT_STYLE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_SUBTITLE_LANGUAGE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_SUBTITLE_SIZE_DIALOG
import com.flixclusive.feature.mobile.settings.component.dialog.LanguageDialog
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.dialog.SubtitleDialogEdgeType
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.dialog.SubtitleDialogFontBackgroundColor
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.dialog.SubtitleDialogFontColor
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.dialog.SubtitleDialogFontStyle
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.dialog.SubtitleDialogSize
import com.flixclusive.feature.mobile.settings.util.AppSettingsHelper.rememberAppSettingsChanger
import com.flixclusive.feature.mobile.settings.util.AppSettingsHelper.rememberLocalAppSettings
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun SubtitleDialogWrapper(
    openedDialogMap: Map<String, Boolean>,
    onDismissDialog: (String) -> Unit
) {
    val appSettings by rememberLocalAppSettings()
    val onChangeSettings by rememberAppSettingsChanger()
    
    when {
        openedDialogMap[KEY_SUBTITLE_SIZE_DIALOG] == true -> {
            SubtitleDialogSize(
                appSettings = appSettings,
                onChange = {
                    onChangeSettings(appSettings.copy(subtitleSize = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_SUBTITLE_SIZE_DIALOG)
                }
            )
        }
        openedDialogMap[KEY_SUBTITLE_LANGUAGE_DIALOG] == true -> {
            LanguageDialog(
                appSettings = appSettings,
                selectedOption = remember { mutableStateOf(appSettings.subtitleLanguage) },
                label = stringResource(id = LocaleR.string.subtitles_language),
                onChange = {
                    onChangeSettings(appSettings.copy(subtitleLanguage = it.language))
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
                    onChangeSettings(appSettings.copy(subtitleEdgeType = it))
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
                    onChangeSettings(appSettings.copy(subtitleFontStyle = it))
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
                    onChangeSettings(appSettings.copy(subtitleColor = it))
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
                    onChangeSettings(appSettings.copy(subtitleBackgroundColor = it))
                },
                onDismissRequest = {
                    onDismissDialog(KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG)
                }
            )
        }
    }
}