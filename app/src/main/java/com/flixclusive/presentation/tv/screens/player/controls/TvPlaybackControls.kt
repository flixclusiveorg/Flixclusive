package com.flixclusive.presentation.tv.screens.player.controls

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.TrackSelectionParameters
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.presentation.common.PlayerUiState
import com.flixclusive.presentation.mobile.common.composables.GradientCircularProgressIndicator
import com.flixclusive.presentation.tv.utils.PlayerTvUtils.getTimeToSeekToBasedOnSeekMultiplier
import com.flixclusive.presentation.utils.FormatterUtils.formatMinSec
import com.flixclusive.presentation.utils.PlayerUiUtils.LocalPlayer
import com.flixclusive.providers.models.common.Subtitle
import com.flixclusive.providers.models.common.VideoDataServer

@Composable
fun TvPlaybackControls(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isTvShow: Boolean,
    servers: List<VideoDataServer>,
    subtitles: List<Subtitle>,
    qualities: List<String>,
    stateProvider: () -> PlayerUiState,
    dialogStateProvider: () -> VideoDataDialogState,
    sideSheetFocusPriority: BottomControlsButtonType?,
    playbackTitle: String,
    isLastEpisode: Boolean,
    seekMultiplier: Long,
    onSideSheetDismiss: (BottomControlsButtonType?) -> Unit,
    showControls: (Boolean) -> Unit,
    onSeekMultiplierChange: (Long) -> Unit,
    onPauseToggle: () -> Unit,
    onBack: () -> Unit,
    onNextEpisode: () -> Unit,
    onSubtitleChange: (Int, TrackSelectionParameters) -> TrackSelectionParameters,
    onVideoQualityChange: (Int, TrackSelectionParameters) -> TrackSelectionParameters?,
) {
    val player = LocalPlayer.current

    val state by rememberUpdatedState(stateProvider())
    val dialogState by rememberUpdatedState(dialogStateProvider())

    val isLoading = remember(state, dialogState, seekMultiplier) {
        state.playbackState == STATE_BUFFERING && seekMultiplier == 0L || dialogState !is VideoDataDialogState.Success
    }

    val topFadeEdge = Brush.verticalGradient(
        0F to Color.Black,
        0.9F to Color.Transparent
    )
    val bottomFadeEdge = Brush.verticalGradient(
        0F to Color.Transparent,
        0.9F to Color.Black
    )

    val isInHours = remember(state.totalDuration) {
        state.totalDuration.formatMinSec().count { it == ':' } == 2
    }
    val seekText by remember(seekMultiplier) {
        derivedStateOf {
            val symbol = if (seekMultiplier > 0) "+"
            else if (seekMultiplier < 0) "-"
            else return@derivedStateOf ""

            val timeToFormat = getTimeToSeekToBasedOnSeekMultiplier(
                currentTime = state.currentTime,
                maxDuration = state.totalDuration,
                seekMultiplier = seekMultiplier
            )

            symbol + timeToFormat.formatMinSec(isInHours)
        }
    }

    val selectedSubtitle = remember(state.selectedSubtitle, subtitles.lastOrNull()?.url) {
        try {
            subtitles[state.selectedSubtitle].lang
        } catch (_: Exception) {
            "Subtitle Error!"
        }
    }
    val selectedQuality = remember(state.selectedQuality, qualities.size) {
        try {
            qualities[state.selectedQuality]
        } catch (_: Exception) {
            null
        }
    }
    val selectedServer = remember(state.selectedServer, servers.size) {
        try {
            servers[state.selectedServer].serverName
        } catch (_: Exception) {
            "Server Error!"
        }
    }

    BackHandler(enabled = !isVisible) {
        showControls(true)
    }

    Box(
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { fullHeight: Int ->
                    -fullHeight
                }
            ),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight: Int ->
                    -fullHeight
                }
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TvTopControls(
                modifier = Modifier
                    .drawBehind {
                        drawRect(brush = topFadeEdge)
                    },
                isTvShow = isTvShow,
                isLastEpisode = isLastEpisode,
                title = playbackTitle,
                extendControlsVisibility = { showControls(true) },
                onNavigationIconClick = onBack,
                onNextEpisodeClick = onNextEpisode,
                onVideoSettingsClick = { /* TODO */ }
            )
        }

        AnimatedVisibility(
            visible = seekMultiplier != 0L,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = seekText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 65.sp
                ),
                color = Color.White
            )
        }

        AnimatedVisibility(
            visible = isLoading,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GradientCircularProgressIndicator(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                )
            )
        }

        AnimatedVisibility(
            visible = isVisible || seekMultiplier > 0,
            enter = slideInVertically(
                initialOffsetY = { fullHeight: Int ->
                    fullHeight
                }
            ),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight: Int ->
                    fullHeight
                }
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            TvBottomControls(
                modifier = Modifier
                    .drawBehind {
                        drawRect(brush = bottomFadeEdge)
                    },
                state = state,
                isSeeking = seekMultiplier > 0,
                onSeekMultiplierChange = onSeekMultiplierChange,
                subtitle = selectedSubtitle,
                quality = selectedQuality,
                server = selectedServer,
                extendControlsVisibility = { showControls(true) },
                showSideSheetPanel = {
                    showControls(false)
                    onSideSheetDismiss(it)
                },
                onPauseToggle = {
                    player?.run {
                        when {
                            isPlaying -> pause()
                            !isPlaying && playbackState == Player.STATE_ENDED -> {
                                seekTo(0)
                                playWhenReady = true
                            }

                            else -> play()
                        }
                        onPauseToggle()
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = sideSheetFocusPriority != null,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            BackHandler {
                showControls(true)
                onSideSheetDismiss(null)
            }

            SlidingPlayerSheet(
                sideSheetFocusPriority = sideSheetFocusPriority,
                subtitles = subtitles,
                qualities = qualities,
                servers = servers,
                selectedSubtitle = state.selectedSubtitle,
                selectedQuality = state.selectedQuality,
                selectedServer = state.selectedServer,
                onSubtitleChange = {
                    player?.run {
                        trackSelectionParameters = onSubtitleChange(
                            it,
                            trackSelectionParameters
                        )
                    }
                },
                onQualityChange = {
                    player?.run {
                        trackSelectionParameters = onVideoQualityChange(
                            it,
                            trackSelectionParameters
                        ) ?: return@run
                    }
                },
                onDismissSheet = {
                    showControls(true)
                    onSideSheetDismiss(null)
                }
            )
        }
    }
}