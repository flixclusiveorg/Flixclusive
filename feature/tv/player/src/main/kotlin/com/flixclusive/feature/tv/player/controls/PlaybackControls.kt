@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.flixclusive.feature.tv.player.controls

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NonInteractiveSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.core.ui.player.FlixclusivePlayerManager
import com.flixclusive.core.ui.player.PlayerUiState
import com.flixclusive.core.ui.player.util.PlayerCacheManager
import com.flixclusive.core.ui.player.util.PlayerUiUtil
import com.flixclusive.core.ui.player.util.PlayerUiUtil.formatMinSec
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.provider.SourceDataState
import com.flixclusive.model.provider.SourceLink
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import kotlin.math.abs

@Composable
internal fun PlaybackControls(
    modifier: Modifier = Modifier,
    appSettings: AppSettings,
    isVisible: Boolean,
    isTvShow: Boolean,
    servers: List<SourceLink>,
    stateProvider: () -> PlayerUiState,
    dialogStateProvider: () -> SourceDataState,
    playbackTitle: String,
    isLastEpisode: Boolean,
    seekMultiplier: Long,
    onSideSheetDismiss: (BottomControlsButtonType?) -> Unit,
    showControls: (Boolean) -> Unit,
    onSeekMultiplierChange: (Long) -> Unit,
    onBack: () -> Unit,
    onNextEpisode: () -> Unit,
) {
    val player by rememberLocalPlayerManager()

    val state by rememberUpdatedState(stateProvider())
    val dialogState by rememberUpdatedState(dialogStateProvider())

    val isLoading = remember(player.playbackState, dialogState, seekMultiplier) {
        player.playbackState == Player.STATE_BUFFERING
        && seekMultiplier == 0L
        || dialogState !is SourceDataState.Success
    }

    val topFadeEdge = Brush.verticalGradient(
        0F to Color.Black,
        0.9F to Color.Transparent
    )
    val bottomFadeEdge = Brush.verticalGradient(
        0F to Color.Transparent,
        0.9F to Color.Black
    )

    val isInHours = remember(player.duration) {
        player.duration.formatMinSec().count { it == ':' } == 2
    }
    val isSeeking = remember(seekMultiplier) { seekMultiplier != 0L }
    var seekPosition by remember { mutableLongStateOf(player.currentPosition) }

    val seekingTimeInReverse = remember(seekMultiplier) {
        val timeToSeekTo = appSettings.preferredSeekAmount * seekMultiplier

        "-" + abs((player.duration - (seekPosition + timeToSeekTo))).formatMinSec(isInHours)
    }
    val seekingTime = remember(seekMultiplier) {
        val timeToSeekTo = appSettings.preferredSeekAmount * seekMultiplier

        (seekPosition + timeToSeekTo).formatMinSec(isInHours)
    }

    LaunchedEffect(isSeeking) {
        seekPosition = player.currentPosition
    }

    LaunchedEffect(seekMultiplier) {
        var shouldPlayAfterSeek = false

        showControls(true)
        if(player.isPlaying) {
            player.pause()
            shouldPlayAfterSeek = true
        }

        if (seekMultiplier != 0L) {
            delay(2500)
            val timeToSeekTo = appSettings.preferredSeekAmount * seekMultiplier
            player.seekTo(seekPosition + timeToSeekTo)

            onSeekMultiplierChange(0)
        }

        if(shouldPlayAfterSeek)
            player.play()
    }

    val selectedSubtitle = remember(
        player.selectedSubtitleIndex,
        player.availableSubtitles.size
    ) {
        player.availableSubtitles.getOrNull(player.selectedSubtitleIndex)?.language
    }

    val selectedAudio = remember(
        player.selectedAudio,
        player.availableAudios.size
    ) {
        if (player.availableAudios.size == 1) {
            null
        } else player.availableAudios.getOrNull(player.selectedAudio)
    }

    BackHandler(enabled = !isVisible) {
        if (isSeeking) {
            onSeekMultiplierChange(0)
            return@BackHandler
        }

        showControls(true)
    }

    Box(
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = isVisible && !isSeeking,
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
            TopControls(
                modifier = Modifier.drawBehind {
                    drawRect(topFadeEdge)
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
            visible = isSeeking,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = if (appSettings.isPlayerTimeReversed) seekingTimeInReverse else seekingTime,
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
            visible = isVisible || isSeeking,
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
            BottomControls(
                modifier = Modifier.drawBehind {
                    drawRect(brush = bottomFadeEdge)
                },
                isPlayerTimeReversed = appSettings.isPlayerTimeReversed,
                isSeeking = isSeeking,
                selectedServer = servers.getOrNull(state.selectedSourceLink)?.name ?: "Default Server",
                selectedSubtitle = selectedSubtitle,
                selectedAudio = selectedAudio,
                onSeekMultiplierChange = onSeekMultiplierChange,
                extendControlsVisibility = { showControls(true) },
                showSideSheetPanel = {
                    showControls(false)
                    onSideSheetDismiss(it)
                },
            )
        }
    }
}


@Preview(device = "id:tv_1080p")
@Composable
private fun PlaybackControlsPreview() {
    var seekMultiplier by remember { mutableLongStateOf(0L) }

    FlixclusiveTheme(isTv = true) {
        Surface(
            colors = NonInteractiveSurfaceDefaults.colors(Color.Black),
            modifier = Modifier
                .fillMaxSize()
        ) {
            CompositionLocalProvider(
                PlayerUiUtil.LocalPlayerManager provides
                        FlixclusivePlayerManager(
                            OkHttpClient(),
                            LocalContext.current,
                            PlayerCacheManager(LocalContext.current),
                            AppSettings()
                        )
            ) {
                PlaybackControls(
                    isVisible = true,
                    isTvShow = true,
                    appSettings = AppSettings(isPlayerTimeReversed = false),
                    servers = emptyList(),
                    stateProvider = { PlayerUiState() },
                    dialogStateProvider = { SourceDataState.Success },
                    playbackTitle = "American Bad Boy [$seekMultiplier]",
                    isLastEpisode = false,
                    seekMultiplier = seekMultiplier,
                    onSideSheetDismiss = {},
                    showControls = {},
                    onSeekMultiplierChange = {
                        if (it == 0L) {
                            seekMultiplier = 0L
                            return@PlaybackControls
                        }

                        seekMultiplier += it
                    },
                    onBack = { },
                    onNextEpisode = {},
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}