package com.flixclusive.feature.mobile.settings.settings_group

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.settings.KEY_AUDIO_LANGUAGE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_QUALITY_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_RESIZE_MODE_DIALOG
import com.flixclusive.feature.mobile.settings.KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.feature.mobile.settings.util.AppSettingsHelper.rememberLocalAppSettings
import com.flixclusive.feature.mobile.settings.util.AppSettingsHelper.rememberAppSettingsChanger
import com.flixclusive.model.datastore.player.ResizeMode
import java.util.Locale
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun currentVideoPlayerSettings(
    cacheLinksSize: Int,
    clearCacheLinks: () -> Unit
): List<SettingsItem> {
    val appSettings by rememberLocalAppSettings()
    val onChangeSettings by rememberAppSettingsChanger()

    val selectedResizeMode = remember(appSettings.preferredResizeMode) {
        ResizeMode.entries.find { it.ordinal == appSettings.preferredResizeMode }.toString()
    }

    return listOf(
        SettingsItem(
            title = stringResource(LocaleR.string.release_player),
            description = stringResource(LocaleR.string.release_player_desc),
            onClick = {
                onChangeSettings(appSettings.copy(shouldReleasePlayer = !appSettings.shouldReleasePlayer))
            },
            previewContent = {
                Switch(
                    checked = appSettings.shouldReleasePlayer,
                    onCheckedChange = {
                        onChangeSettings(appSettings.copy(shouldReleasePlayer = it))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            }
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.reverse_player_time),
            onClick = {
                onChangeSettings(appSettings.copy(isPlayerTimeReversed = !appSettings.isPlayerTimeReversed))
            },
            previewContent = {
                Switch(
                    checked = appSettings.isPlayerTimeReversed,
                    onCheckedChange = {
                        onChangeSettings(appSettings.copy(isPlayerTimeReversed = !appSettings.isPlayerTimeReversed))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            }
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.pip_mode),
            description = stringResource(LocaleR.string.pip_mode_desc),
            onClick = {
                onChangeSettings(appSettings.copy(isPiPModeEnabled = !appSettings.isPiPModeEnabled))
            },
            previewContent = {
                Switch(
                    checked = appSettings.isPiPModeEnabled,
                    onCheckedChange = {
                        onChangeSettings(appSettings.copy(isPiPModeEnabled = !appSettings.isPiPModeEnabled))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            }
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.volume_booster),
            description = stringResource(LocaleR.string.volume_booster_desc),
            onClick = {
                onChangeSettings(appSettings.copy(isUsingVolumeBoost = !appSettings.isUsingVolumeBoost))
            },
            previewContent = {
                Switch(
                    checked = appSettings.isUsingVolumeBoost,
                    onCheckedChange = {
                        onChangeSettings(appSettings.copy(isUsingVolumeBoost = !appSettings.isUsingVolumeBoost))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            }
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.clear_cache_links),
            description = stringResource(LocaleR.string.cache_links_item_count, cacheLinksSize),
            onClick = clearCacheLinks,
            previewContent = {
                Icon(
                    painter = painterResource(id = UiCommonR.drawable.database_icon),
                    contentDescription = stringResource(id = LocaleR.string.clear_cache_content_desc),
                    tint = LocalContentColor.current.onMediumEmphasis(0.8F)
                )
            }
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.preferred_audio_language),
            description = Locale(appSettings.preferredAudioLanguage).displayLanguage,
            dialogKey = KEY_AUDIO_LANGUAGE_DIALOG,
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.preferred_quality),
            description = appSettings.preferredQuality.qualityName.asString(),
            dialogKey = KEY_PLAYER_QUALITY_DIALOG,
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.preferred_resize_mode),
            description = selectedResizeMode,
            dialogKey = KEY_PLAYER_RESIZE_MODE_DIALOG,
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.seek_length_label),
            description = "${appSettings.preferredSeekAmount / 1000} seconds",
            dialogKey = KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG,
        ),
    )
}