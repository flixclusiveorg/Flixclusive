package com.flixclusive.feature.mobile.player.controls.dialogs.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.PlayerSnackbarMessageType
import com.flixclusive.core.ui.player.PlayerUiState
import com.flixclusive.core.ui.player.util.PlayerUiUtil.availablePlaybackSpeeds
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.feature.mobile.player.controls.common.BasePlayerDialog
import com.flixclusive.feature.mobile.player.controls.common.PlayerDialogButton
import com.flixclusive.model.datastore.player.ResizeMode
import com.flixclusive.core.ui.player.R as PlayerR
import com.flixclusive.core.locale.R as LocaleR

internal const val ELEVATED_VIDEO_SETTINGS_PANEL = 0.4F

internal data class VideoSettingItem(
    @DrawableRes val iconId: Int,
    @StringRes val labelId: Int,
    val selected: Int = -1,
    val onClick: ((Int, String) -> Unit)? = null,
    val items: List<String>? = null,
    val content: (@Composable () -> Unit)? = null,
)

@Composable
internal fun PlayerSettingsDialog(
    state: PlayerUiState,
    showSnackbar: (String, Int, PlayerSnackbarMessageType) -> Unit,
    onResizeModeChange: (Int) -> Unit,
    onPanelChange: (Int) -> Unit,
    onDismissSheet: () -> Unit,
) {
    val player by rememberLocalPlayerManager()

    val resizeModes = remember { ResizeMode.entries.map { it.toString() } }

    val settingsList = listOf(
        VideoSettingItem(
            iconId = PlayerR.drawable.speedometer,
            labelId = LocaleR.string.playback_speed,
            items = availablePlaybackSpeeds.map { "${it}x" },
            selected = availablePlaybackSpeeds.indexOf(player.playbackSpeed),
            onClick = { i, _ ->
                player.onPlaybackSpeedChange(i)

                showSnackbar(
                    "${player.playbackSpeed}x",
                    LocaleR.string.playback_speed_snackbar_message,
                    PlayerSnackbarMessageType.PlaybackSpeed
                )
            }
        ),
        VideoSettingItem(
            iconId = PlayerR.drawable.resize_mode_icon,
            labelId = LocaleR.string.resize_mode,
            items = resizeModes,
            selected = state.selectedResizeMode,
            onClick = { i, _ ->
                onResizeModeChange(i)
            }
        ),
        VideoSettingItem(
            iconId = PlayerR.drawable.sync_black_24dp,
            labelId = LocaleR.string.sync_subtitles,
            content = {
                SubtitleSyncPanel()
            }
        )
    )

    BasePlayerDialog(onDismissSheet = onDismissSheet) {
        Row {
            PlayerSettingsList(
                modifier = Modifier
                    .weight(1F)
                    .padding(
                        top = 15.dp,
                        start = 15.dp,
                        bottom = 15.dp,
                    ),
                settingsList = settingsList,
                selectedPanel = settingsList[state.lastOpenedPanel],
                onPanelChange = onPanelChange,
            )

            Box(
                modifier = Modifier
                    .weight(1F)
                    .fillMaxHeight(0.85F)
                    .padding(15.dp)
                    .border(
                        width = 1.dp,
                        color = Color.White.onMediumEmphasis(emphasis = ELEVATED_VIDEO_SETTINGS_PANEL),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .background(
                        color = Color.Black.onMediumEmphasis(emphasis = ELEVATED_VIDEO_SETTINGS_PANEL),
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                AnimatedContent(
                    targetState = state.lastOpenedPanel,
                    label = "",
                    transitionSpec = {
                        ContentTransform(
                            targetContentEnter = fadeIn(),
                            initialContentExit = fadeOut()
                        )
                    }
                ) { index ->
                    val item = settingsList[index]

                    PlayerSettingsPanel(
                        modifier = Modifier.fillMaxSize(),
                        items = item.items,
                        selected = item.selected,
                        content = item.content,
                        onClick = item.onClick,
                    )
                }
            }
        }

        PlayerDialogButton(
            modifier = Modifier
                .align(Alignment.BottomEnd),
            label = stringResource(LocaleR.string.close_label),
            onClick = onDismissSheet
        )
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun PlayerSettingsDialogPreview() {
    FlixclusiveTheme {
        Surface {
            PlayerSettingsDialog(
                state = PlayerUiState(),
                showSnackbar = {_, _, _ -> },
                onResizeModeChange = {},
                onPanelChange = {}
            ) {

            }
        }
    }
}

