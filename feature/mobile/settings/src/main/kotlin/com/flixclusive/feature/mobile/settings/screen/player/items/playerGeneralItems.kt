package com.flixclusive.feature.mobile.settings.screen.player.items

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_AUDIO_LANGUAGE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_QUALITY_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_RESIZE_MODE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.rememberLocalAppSettings
import com.flixclusive.model.datastore.player.ResizeMode
import java.util.Locale
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun playerGeneralItems(
    cacheLinksSize: Int,
    clearCacheLinks: () -> Unit
): List<SettingsItem> {
    val appSettings by rememberLocalAppSettings()

    val selectedSeekAmount = remember(appSettings.preferredSeekAmount) {
        "${appSettings.preferredSeekAmount / 1000} seconds"
    }
    val selectedResizeMode = remember(appSettings.preferredResizeMode) {
        ResizeMode.entries.find { it.ordinal == appSettings.preferredResizeMode }.toString()
    }

    return listOf(
        SettingsItem(
            title = stringResource(LocaleR.string.seek_length_label),
            description = selectedSeekAmount,
            dialogKey = KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG,
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
        )
    )
}