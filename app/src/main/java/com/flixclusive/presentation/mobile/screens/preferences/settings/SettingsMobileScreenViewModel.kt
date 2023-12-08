package com.flixclusive.presentation.mobile.screens.preferences.settings

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.AppSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val KEY_PREFERRED_SERVER_DIALOG = "isPreferredServerDialogOpen"
const val KEY_SUBTITLE_LANGUAGE_DIALOG = "isSubtitleLanguageDialogOpen"
const val KEY_SUBTITLE_COLOR_DIALOG = "isSubtitleColorDialogOpen"
const val KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG = "isSubtitleBackgroundDialogOpen"
const val KEY_SUBTITLE_SIZE_DIALOG = "isSubtitleSizeDialogOpen"
const val KEY_SUBTITLE_FONT_STYLE_DIALOG = "isSubtitleFontStyleDialogOpen"
const val KEY_SUBTITLE_EDGE_TYPE_DIALOG = "isSubtitleEdgeTypeDialogOpen"
const val KEY_VIDEO_PLAYER_SERVER_DIALOG = "isVideoPlayerServerDialogOpen"
const val KEY_VIDEO_PLAYER_QUALITY_DIALOG = "isVideoPlayerQualityDialogOpen"
const val KEY_DOH_DIALOG = "isDoHDialogOpen"

@HiltViewModel
class SettingsMobileScreenViewModel @Inject constructor(
    private val appSettingsManager: AppSettingsManager,
) : ViewModel() {

    val openedDialogMap = mutableStateMapOf(
        KEY_PREFERRED_SERVER_DIALOG to false,
        KEY_SUBTITLE_LANGUAGE_DIALOG to false,
        KEY_SUBTITLE_COLOR_DIALOG to false,
        KEY_SUBTITLE_SIZE_DIALOG to false,
        KEY_SUBTITLE_FONT_STYLE_DIALOG to false,
        KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG to false,
        KEY_SUBTITLE_EDGE_TYPE_DIALOG to false,
        KEY_VIDEO_PLAYER_SERVER_DIALOG to false,
        KEY_VIDEO_PLAYER_QUALITY_DIALOG to false,
        KEY_DOH_DIALOG to false,
    )

    val appSettings = appSettingsManager.appSettings.data
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            appSettingsManager.localAppSettings
        )

    fun toggleDialog(dialogKey: String) {
        openedDialogMap[dialogKey] = !openedDialogMap[dialogKey]!!
    }

    fun onChangeSettings(newAppSettings: AppSettings) {
        viewModelScope.launch {
            appSettingsManager.updateData(newAppSettings)
        }
    }
}
