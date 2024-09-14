package com.flixclusive.feature.mobile.settings.settings_group

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.flixclusive.feature.mobile.settings.KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_SUBTITLE_COLOR_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_SUBTITLE_EDGE_TYPE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_SUBTITLE_FONT_STYLE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_SUBTITLE_LANGUAGE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_SUBTITLE_SIZE_DIALOG
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.BorderedText
import com.flixclusive.feature.mobile.settings.util.ColorPickerHelper
import com.flixclusive.feature.mobile.settings.util.AppSettingsHelper.rememberLocalAppSettings
import com.flixclusive.feature.mobile.settings.util.AppSettingsHelper.rememberAppSettingsChanger
import com.flixclusive.model.datastore.player.CaptionEdgeTypePreference
import com.flixclusive.model.datastore.player.CaptionStylePreference
import java.util.Locale
import com.flixclusive.core.locale.R as LocaleR

private const val DEFAULT_TEXT_PREVIEW = "Abc"

@Composable
internal fun currentSubtitlesSettings(): List<SettingsItem> {
    val appSettings by rememberLocalAppSettings()
    val onChangeSettings by rememberAppSettingsChanger()

    return listOf(
        SettingsItem(
            title = stringResource(LocaleR.string.subtitle),
            description = stringResource(id = LocaleR.string.subtitles_toggle_desc),
            onClick = {
                onChangeSettings(appSettings.copy(isSubtitleEnabled = !appSettings.isSubtitleEnabled))
            },
            previewContent = {
                Switch(
                    checked = appSettings.isSubtitleEnabled,
                    onCheckedChange = {
                        onChangeSettings(appSettings.copy(isSubtitleEnabled = it))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            }
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.subtitles_language),
            description = Locale(appSettings.subtitleLanguage).displayLanguage,
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_LANGUAGE_DIALOG,
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.subtitles_size),
            description = appSettings.subtitleSize.toString(),
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_SIZE_DIALOG
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.subtitles_font_style),
            description = appSettings.subtitleFontStyle.toString(),
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_FONT_STYLE_DIALOG,
            previewContent = {
                Text(
                    text = DEFAULT_TEXT_PREVIEW,
                    style = MaterialTheme.typography.labelLarge.run {
                        when (appSettings.subtitleFontStyle) {
                            CaptionStylePreference.Normal -> copy(
                                fontWeight = FontWeight.Normal
                            )

                            CaptionStylePreference.Bold -> copy(
                                fontWeight = FontWeight.Bold
                            )

                            CaptionStylePreference.Italic -> copy(
                                fontStyle = FontStyle.Italic
                            )

                            CaptionStylePreference.Monospace -> copy(
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                )
            }
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.subtitles_color),
            description = stringResource(LocaleR.string.subtitles_color_desc),
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_COLOR_DIALOG,
            previewContent = {
                ColorPickerHelper.BoxWithColor(
                    if (appSettings.isSubtitleEnabled) appSettings.subtitleColor
                    else Color.Gray.toArgb()
                )
            }
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.subtitles_background_color),
            description = stringResource(LocaleR.string.subtitles_background_color_desc),
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG,
            previewContent = {
                ColorPickerHelper.BoxWithColor(
                    if (appSettings.isSubtitleEnabled) appSettings.subtitleBackgroundColor
                    else Color.Gray.toArgb()
                )
            }
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.subtitles_edge_type),
            description = appSettings.subtitleEdgeType.toString(),
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_EDGE_TYPE_DIALOG,
            previewContent = {
                when (appSettings.subtitleEdgeType) {
                    CaptionEdgeTypePreference.Drop_Shadow -> {
                        Text(
                            text = DEFAULT_TEXT_PREVIEW,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium,
                                shadow = Shadow(
                                    offset = Offset(6F, 6F),
                                    blurRadius = 3f,
                                    color = Color(appSettings.subtitleEdgeType.color),
                                ),
                            )
                        )
                    }

                    CaptionEdgeTypePreference.Outline -> {
                        BorderedText(
                            text = DEFAULT_TEXT_PREVIEW,
                            borderColor = Color(appSettings.subtitleEdgeType.color),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium,
                            )
                        )
                    }
                }
            }
        ),
    )
}