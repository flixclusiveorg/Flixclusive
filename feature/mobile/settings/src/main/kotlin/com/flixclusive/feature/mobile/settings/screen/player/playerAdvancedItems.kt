package com.flixclusive.feature.mobile.settings.screen.player

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_DECODER_PRIORITY_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_BUFFER_LENGTH_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_BUFFER_SIZE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_DISK_CACHE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.rememberAppSettingsChanger
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.rememberLocalAppSettings
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun advancedPlayerItems(
    sizeSummary: String?,
    updateAppCacheSize: () -> Unit,
): List<SettingsItem> {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current


    val appSettings by rememberLocalAppSettings()
    val onChangeSettings by rememberAppSettingsChanger()

    return listOf(
        SettingsItem(
            title = stringResource(LocaleR.string.release_player),
            description = stringResource(LocaleR.string.release_player_desc),
            onClick = {
                onChangeSettings(appSettings.copy(shouldReleasePlayer = !appSettings.shouldReleasePlayer))
            },
            content = {
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
            title = stringResource(LocaleR.string.volume_booster),
            description = stringResource(LocaleR.string.volume_booster_desc),
            onClick = {
                onChangeSettings(appSettings.copy(isUsingVolumeBoost = !appSettings.isUsingVolumeBoost))
            },
            content = {
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
            title = stringResource(LocaleR.string.decoder_priority),
            description = stringResource(LocaleR.string.decoder_priority_description),
            dialogKey = KEY_DECODER_PRIORITY_DIALOG,
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.video_cache_size),
            description = stringResource(LocaleR.string.video_cache_size_label),
            dialogKey = KEY_PLAYER_DISK_CACHE_DIALOG,
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.video_buffer_size),
            description = stringResource(LocaleR.string.video_buffer_size_label),
            dialogKey = KEY_PLAYER_BUFFER_SIZE_DIALOG,
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.video_buffer_max_length),
            description = stringResource(LocaleR.string.video_buffer_max_length_desc),
            dialogKey = KEY_PLAYER_BUFFER_LENGTH_DIALOG,
        ),
        SettingsItem(
            title = stringResource(LocaleR.string.clear_app_cache),
            description = sizeSummary,
            onClick = {
                safeCall {
                    scope.launch { context.cacheDir.deleteRecursively() }
                    updateAppCacheSize()
                }
            },
            content = {
                Icon(
                    painter = painterResource(id = UiCommonR.drawable.broom_clean),
                    contentDescription = stringResource(id = LocaleR.string.clear_cache_content_desc),
                    tint = LocalContentColor.current.onMediumEmphasis(0.8F)
                )
            }
        ),
    )
}