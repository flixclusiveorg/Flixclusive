package com.flixclusive.feature.mobile.settings

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.data.search_history.SearchHistoryRepository
import com.flixclusive.domain.provider.GetMediaLinksUseCase
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.AppSettingsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
internal class SettingsScreenViewModel @Inject constructor(
    private val appSettingsManager: AppSettingsManager,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val sourceLinksProvider: GetMediaLinksUseCase
) : ViewModel() {
    val searchHistoryCount = searchHistoryRepository.getAllItemsInFlow()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val openedDialogMap = mutableStateMapOf(
        KEY_PREFERRED_SERVER_DIALOG to false,
        KEY_SUBTITLE_LANGUAGE_DIALOG to false,
        KEY_SUBTITLE_COLOR_DIALOG to false,
        KEY_SUBTITLE_SIZE_DIALOG to false,
        KEY_SUBTITLE_FONT_STYLE_DIALOG to false,
        KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG to false,
        KEY_SUBTITLE_EDGE_TYPE_DIALOG to false,
        KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG to false,
        KEY_PLAYER_QUALITY_DIALOG to false,
        KEY_PLAYER_RESIZE_MODE_DIALOG to false,
        KEY_DOH_DIALOG to false,
        KEY_PLAYER_DISK_CACHE_DIALOG to false,
        KEY_PLAYER_BUFFER_SIZE_DIALOG to false,
        KEY_PLAYER_BUFFER_LENGTH_DIALOG to false,
        KEY_SEARCH_HISTORY_NOTICE_DIALOG to false,
        KEY_AUDIO_LANGUAGE_DIALOG to false,
        KEY_DECODER_PRIORITY_DIALOG to false,
    )

    val appSettings = appSettingsManager.appSettings.data
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            appSettingsManager.cachedAppSettings
        )

    val appSettingsProvider = appSettingsManager.providerSettings.data
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            appSettingsManager.cachedProviderSettings
        )

    val cacheLinksSize by derivedStateOf {
        sourceLinksProvider.cache.size
    }

    fun toggleDialog(dialogKey: String) {
        openedDialogMap[dialogKey] = !openedDialogMap[dialogKey]!!
    }

    fun onChangeAppSettings(newAppSettings: AppSettings) {
        viewModelScope.launch {
            appSettingsManager.updateSettings(newAppSettings)
        }
    }

    fun onChangeAppSettingsProvider(newAppSettingsProvider: AppSettingsProvider) {
        viewModelScope.launch {
            appSettingsManager.updateProviderSettings {
                newAppSettingsProvider
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearAll()
        }
    }

    fun clearCacheLinks() {
        sourceLinksProvider.cache.clear()
    }
}
