package com.flixclusive.feature.mobile.settings.util

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberUpdatedState
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.AppSettingsProvider

internal object LocalProviderHelper {
    val LocalAppSettings = compositionLocalOf { AppSettings() }
    val LocalAppSettingsChanger = compositionLocalOf<((AppSettings) -> Unit)?> { null }

    val LocalAppSettingsProvider = compositionLocalOf { AppSettingsProvider() }
    val LocalAppSettingsProviderChanger = compositionLocalOf<((AppSettingsProvider) -> Unit)?> { null }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    val LocalScaffoldNavigator = compositionLocalOf<ThreePaneScaffoldNavigator<BaseTweakScreen>?> { null }

    val LocalDialogKeyMap = compositionLocalOf { mutableStateMapOf<String, Boolean>() }

    @Composable
    fun rememberLocalAppSettings() = rememberUpdatedState(LocalAppSettings.current)

    @Composable
    fun rememberAppSettingsChanger(): State<((AppSettings) -> Unit)> {
        val localSettingsChanger = LocalAppSettingsChanger.current
        check(localSettingsChanger != null) {
            "LocalAppSettingsChanger not provided"
        }

        return rememberUpdatedState(localSettingsChanger)
    }

    @Composable
    fun rememberLocalAppSettingsProvider() = rememberUpdatedState(LocalAppSettingsProvider.current)

    @Composable
    fun rememberAppSettingsProviderChanger(): State<((AppSettingsProvider) -> Unit)> {
        val localSettingsChanger = LocalAppSettingsProviderChanger.current
        check(localSettingsChanger != null) {
            "LocalSettingsChanger not provided"
        }

        return rememberUpdatedState(localSettingsChanger)
    }

    const val KEY_PREFERRED_SERVER_DIALOG = "isPreferredServerDialogOpen"
    const val KEY_SUBTITLE_LANGUAGE_DIALOG = "isSubtitleLanguageDialogOpen"
    const val KEY_SUBTITLE_COLOR_DIALOG = "isSubtitleColorDialogOpen"
    const val KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG = "isSubtitleBackgroundDialogOpen"
    const val KEY_SUBTITLE_SIZE_DIALOG = "isSubtitleSizeDialogOpen"
    const val KEY_SUBTITLE_FONT_STYLE_DIALOG = "isSubtitleFontStyleDialogOpen"
    const val KEY_SUBTITLE_EDGE_TYPE_DIALOG = "isSubtitleEdgeTypeDialogOpen"
    const val KEY_PLAYER_QUALITY_DIALOG = "isPlayerQualityDialogOpen"
    const val KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG = "isPlayerSeekIncrementDialogOpen"
    const val KEY_PLAYER_RESIZE_MODE_DIALOG = "isPlayerResizeModeDialogOpen"
    const val KEY_PLAYER_BUFFER_LENGTH_DIALOG = "isPlayerBufferLengthOpen"
    const val KEY_PLAYER_BUFFER_SIZE_DIALOG = "isPlayerBufferSizeDialogOpen"
    const val KEY_PLAYER_DISK_CACHE_DIALOG = "isPlayerDiskCacheDialogOpen"
    const val KEY_DOH_DIALOG = "isDoHDialogOpen"
    const val KEY_SEARCH_HISTORY_NOTICE_DIALOG = "isSearchHistoryNoticeDialogOpen"
    const val KEY_AUDIO_LANGUAGE_DIALOG = "isAudioLanguageDialogOpen"
    const val KEY_DECODER_PRIORITY_DIALOG = "isDecoderPriorityDialogOpen"
}