package com.flixclusive.presentation.mobile.screens.player.controls.video_settings_dialog

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import com.flixclusive.R
import com.flixclusive.presentation.common.PlayerUiState
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.providers.models.common.VideoDataServer

const val ELEVATED_VIDEO_SETTINGS_PANEL = 0.4F

data class VideoSettingItem(
    @DrawableRes val iconId: Int,
    @StringRes val labelId: Int,
    val items: List<String>,
    val selected: Int,
    val onClick: (Int, String) -> Unit,
)

val resizeModes = mapOf(
    "Fit" to RESIZE_MODE_FIT,
    "Stretch" to RESIZE_MODE_FILL,
    "Center Crop" to RESIZE_MODE_ZOOM,
)

@Composable
fun VideoSettingsDialog(
    state: PlayerUiState,
    servers: List<VideoDataServer>,
    sources: List<String>,
    onPlaybackSpeedChange: (Int) -> Unit,
    onResizeModeChange: (Int) -> Unit,
    onSourceChange: (String) -> Unit,
    onVideoServerChange: (Int, String) -> Unit,
    onPanelChange: (Int) -> Unit,
    onDismissSheet: () -> Unit,
) {
    val dialogColor = Color.Black.copy(0.6F)
    val dialogShape = MaterialTheme.shapes.medium

    val settingsList = listOf(
        VideoSettingItem(
            iconId = R.drawable.speedometer,
            labelId = R.string.playback_speed,
            items = List(5) { index ->
                if (index == 0) "Normal"
                else "${1F + (index * 0.25F)}x"
            },
            selected = state.selectedPlaybackSpeedIndex,
            onClick = { i, _ ->
                onPlaybackSpeedChange(i)
            }
        ),
        VideoSettingItem(
            iconId = R.drawable.source_db,
            labelId = R.string.source,
            items = sources,
            selected = sources.indexOf(state.selectedSource),
            onClick = { _, source ->
                onSourceChange(source)
            }
        ),
        VideoSettingItem(
            iconId = R.drawable.round_cloud_queue_24,
            labelId = R.string.server,
            items = servers.map { it.serverName },
            selected = state.selectedServer,
            onClick = { i, server ->
                onVideoServerChange(i, server)
            }
        ),
        VideoSettingItem(
            iconId = R.drawable.resize_mode_icon,
            labelId = R.string.resize_mode,
            items = resizeModes.keys.toList(),
            selected = resizeModes.values.indexOf(state.selectedResizeMode),
            onClick = { _, item ->
                onResizeModeChange(resizeModes[item]!!)
            }
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismissSheet()
                },
            contentAlignment = Alignment.Center
        ) {}

        Box(
            modifier = Modifier
                .fillMaxSize(0.8F)
                .clip(dialogShape)
                .background(dialogColor)
        ) {
            Row {
                VideoSettingsList(
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
                            color = colorOnMediumEmphasisMobile(
                                color = Color.White,
                                emphasis = ELEVATED_VIDEO_SETTINGS_PANEL
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .background(
                            color = colorOnMediumEmphasisMobile(
                                color = Color.Black,
                                emphasis = ELEVATED_VIDEO_SETTINGS_PANEL
                            ),
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

                        VideoSettingsPanel(
                            modifier = Modifier.fillMaxSize(),
                            items = item.items,
                            selected = item.selected,
                            onClick = item.onClick,
                        )
                    }
                }
            }

            CloseButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                onClick = onDismissSheet
            )
        }
    }
}

@Composable
private fun CloseButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .padding(5.dp)
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(0.1F),
                contentColor = Color.White
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .heightIn(min = 50.dp)
        ) {
            Text(
                text = stringResource(R.string.close_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Light
            )
        }
    }
}

