package com.flixclusive.feature.mobile.settings

import androidx.compose.runtime.Composable

internal const val KEY_PREFERRED_SERVER_DIALOG = "isPreferredServerDialogOpen"
internal const val KEY_SUBTITLE_LANGUAGE_DIALOG = "isSubtitleLanguageDialogOpen"
internal const val KEY_SUBTITLE_COLOR_DIALOG = "isSubtitleColorDialogOpen"
internal const val KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG = "isSubtitleBackgroundDialogOpen"
internal const val KEY_SUBTITLE_SIZE_DIALOG = "isSubtitleSizeDialogOpen"
internal const val KEY_SUBTITLE_FONT_STYLE_DIALOG = "isSubtitleFontStyleDialogOpen"
internal const val KEY_SUBTITLE_EDGE_TYPE_DIALOG = "isSubtitleEdgeTypeDialogOpen"
internal const val KEY_PLAYER_QUALITY_DIALOG = "isPlayerQualityDialogOpen"
internal const val KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG = "isPlayerSeekIncrementDialogOpen"
internal const val KEY_PLAYER_RESIZE_MODE_DIALOG = "isPlayerResizeModeDialogOpen"
internal const val KEY_PLAYER_BUFFER_LENGTH_DIALOG = "isPlayerBufferLengthOpen"
internal const val KEY_PLAYER_BUFFER_SIZE_DIALOG = "isPlayerBufferSizeDialogOpen"
internal const val KEY_PLAYER_DISK_CACHE_DIALOG = "isPlayerDiskCacheDialogOpen"
internal const val KEY_DOH_DIALOG = "isDoHDialogOpen"
internal const val KEY_SEARCH_HISTORY_NOTICE_DIALOG = "isSearchHistoryNoticeDialogOpen"
internal const val KEY_AUDIO_LANGUAGE_DIALOG = "isAudioLanguageDialogOpen"
internal const val KEY_DECODER_PRIORITY_DIALOG = "isDecoderPriorityDialogOpen"

internal data class SettingsItem(
    val title: String,
    val description: String? = null,
    val enabled: Boolean = true,
    val dialogKey: String? = null,
    val onClick: (() -> Unit)? = null,
    val content: @Composable () -> Unit = {},
)