package com.flixclusive.presentation.mobile.screens.preferences.settings

import androidx.compose.runtime.mutableStateMapOf
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.domain.preferences.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

const val KEY_PREFERRED_SERVER_DIALOG = "isPreferredServerDialogOpen"
const val KEY_SUBTITLE_COLOR_DIALOG = "isSubtitleColorDialogOpen"
const val KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG = "isSubtitleBackgroundDialogOpen"
const val KEY_SUBTITLE_SIZE_DIALOG = "isSubtitleSizeDialogOpen"
const val KEY_SUBTITLE_FONT_STYLE_DIALOG = "isSubtitleFontStyleDialogOpen"
const val KEY_SUBTITLE_EDGE_TYPE_DIALOG = "isSubtitleEdgeTypeDialogOpen"
const val KEY_VIDEO_PLAYER_SERVER_DIALOG = "isVideoPlayerServerDialogOpen"

@HiltViewModel
class SettingsMobileScreenViewModel @Inject constructor(
    private val _appSettings: DataStore<AppSettings>,
) : ViewModel() {

    val openedDialogMap = mutableStateMapOf(
        KEY_PREFERRED_SERVER_DIALOG to false,
        KEY_SUBTITLE_COLOR_DIALOG to false,
        KEY_SUBTITLE_SIZE_DIALOG to false,
        KEY_SUBTITLE_FONT_STYLE_DIALOG to false,
        KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG to false,
        KEY_SUBTITLE_EDGE_TYPE_DIALOG to false,
        KEY_VIDEO_PLAYER_SERVER_DIALOG to false,
    )

    val appSettings = _appSettings.data
        .drop(1)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            runBlocking { _appSettings.data.first() }
        )

    fun toggleDialog(dialogKey: String) {
        openedDialogMap[dialogKey] = !openedDialogMap[dialogKey]!!
    }

    fun onChangeSettings(newAppSettings: AppSettings) {
        viewModelScope.launch {
            _appSettings.updateData { newAppSettings }
        }
    }
}
