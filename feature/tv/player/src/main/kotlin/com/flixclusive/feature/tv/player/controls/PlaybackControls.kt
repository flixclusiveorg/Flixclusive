package com.flixclusive.feature.tv.player.controls

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.Player
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState
import com.flixclusive.core.ui.player.PlayerUiState
import com.flixclusive.core.ui.player.util.PlayerUiUtil.formatMinSec
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.feature.tv.player.controls.settings.AudioAndSubtitlesPanel
import com.flixclusive.feature.tv.player.controls.settings.PlaybackSpeedPanel
import com.flixclusive.feature.tv.player.controls.settings.ServersPanel
import com.flixclusive.feature.tv.player.controls.settings.SubtitleStylePanel
import com.flixclusive.feature.tv.player.controls.settings.SubtitleSyncPanel
import com.flixclusive.model.datastore.user.PlayerPreferences
import com.flixclusive.model.datastore.user.SubtitlesPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.link.Stream
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun PlaybackControls(
    modifier: Modifier = Modifier,
    playerPreferences: PlayerPreferences,
    subtitlesPreferences: SubtitlesPreferences,
    currentEpisodeSelected: Episode?,
    isSubtitleStylePanelOpened: MutableState<Boolean>,
    isSyncSubtitlesPanelOpened: MutableState<Boolean>,
    isAudioAndSubtitlesPanelOpened: MutableState<Boolean>,
    isPlaybackSpeedPanelOpened: MutableState<Boolean>,
    isServerPanelOpened: MutableState<Boolean>,
    isVisible: Boolean,
    isTvShow: Boolean,
    providers: List<ProviderMetadata>,
    servers: List<Stream>,
    stateProvider: () -> PlayerUiState,
    dialogStateProvider: () -> MediaLinkResourceState,
    playbackTitle: String,
    isLastEpisode: Boolean,
    seekMultiplier: Long,
    showControls: (Boolean) -> Unit,
    onSeekMultiplierChange: (Long) -> Unit,
    updatePreferences: (Preferences.Key<String>, UserPreferences) -> Unit,
    onProviderChange: (String) -> Unit,
    onServerChange: (Int) -> Unit,
    onBack: () -> Unit,
    onNextEpisode: () -> Unit,
) {
    val player by rememberLocalPlayerManager()

    val state by rememberUpdatedState(stateProvider())
    val dialogState by rememberUpdatedState(dialogStateProvider())

    val isLoading = remember(player.playbackState, dialogState, seekMultiplier) {
        player.playbackState == Player.STATE_BUFFERING
        && seekMultiplier == 0L
        || !dialogState.isSuccess
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
        val timeToSeekTo = playerPreferences.seekAmount * seekMultiplier

        "-" + abs((player.duration - (seekPosition + timeToSeekTo))).formatMinSec(isInHours)
    }
    val seekingTime = remember(seekMultiplier) {
        val timeToSeekTo = playerPreferences.seekAmount * seekMultiplier

        (seekPosition + timeToSeekTo).formatMinSec(isInHours)
    }

    val noPanelsAreOpen = remember(
        isSubtitleStylePanelOpened.value,
        isSyncSubtitlesPanelOpened.value,
        isAudioAndSubtitlesPanelOpened.value,
        isServerPanelOpened.value,
        isPlaybackSpeedPanelOpened.value,
    ) {
        !isSubtitleStylePanelOpened.value
            && !isSyncSubtitlesPanelOpened.value
            && !isAudioAndSubtitlesPanelOpened.value
            && !isServerPanelOpened.value
            && !isPlaybackSpeedPanelOpened.value
    }

    val areControlsVisible = remember(isVisible, noPanelsAreOpen, isSeeking) {
        isVisible && noPanelsAreOpen && !isSeeking
    }

    val (topSlideEnter, topSlideExit) = slideTransition()
    val (bottomSlideEnter, bottomSlideExit) = slideTransition(
        initialOffsetY = { it },
        targetOffsetY = { it }
    )
    val (bottomHalfSlideEnter, bottomHalfSlideExit) = slideTransition(
        initialOffsetY = { it / 2 },
        targetOffsetY = { it / 2 }
    )

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
            val timeToSeekTo = playerPreferences.seekAmount * seekMultiplier
            player.seekTo(seekPosition + timeToSeekTo)

            onSeekMultiplierChange(0)
        }

        if(shouldPlayAfterSeek)
            player.play()
    }

    BackHandler(enabled = !areControlsVisible) {
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
            visible = areControlsVisible,
            enter = topSlideEnter,
            exit = topSlideExit,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopControls(
                modifier = Modifier.drawBehind {
                    drawRect(topFadeEdge)
                },
                isTvShow = isTvShow,
                isLastEpisode = isLastEpisode,
                title = playbackTitle,
                currentEpisodeSelected = currentEpisodeSelected,
                showControls = { showControls(true) },
                onNavigationIconClick = onBack,
                onNextEpisodeClick = onNextEpisode,
                onServersPanelOpen = { isServerPanelOpened.value = true }
            )
        }

        AnimatedVisibility(
            visible = isSeeking && noPanelsAreOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = if (playerPreferences.isDurationReversed) seekingTimeInReverse else seekingTime,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 65.sp
                ),
                color = Color.White
            )
        }

        AnimatedVisibility(
            visible = isLoading && noPanelsAreOpen,
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
            visible = areControlsVisible || isSeeking,
            enter = bottomSlideEnter,
            exit = bottomSlideExit,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomControls(
                modifier = Modifier.drawBehind {
                    drawRect(brush = bottomFadeEdge)
                },
                isPlayerTimeReversed = playerPreferences.isDurationReversed,
                isSeeking = isSeeking,
                onSeekMultiplierChange = onSeekMultiplierChange,
                showControls = { showControls(true) },
                onSubtitleStylePanelOpen = { isSubtitleStylePanelOpened.value = true },
                onSubtitlesPanelOpen = { isAudioAndSubtitlesPanelOpened.value = true },
                onSyncSubtitlesPanelOpen = { isSyncSubtitlesPanelOpened.value = true },
                onSpeedometerPanelOpen = { isPlaybackSpeedPanelOpened.value = true },
            )
        }

        AnimatedVisibility(
            visible =  isSubtitleStylePanelOpened.value,
            enter = bottomHalfSlideEnter,
            exit = bottomHalfSlideExit,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SubtitleStylePanel(
                subtitlesPreferences = subtitlesPreferences,
                updatePreferences = {
                    updatePreferences(UserPreferences.SUBTITLES_PREFS_KEY, it)
                },
                hidePanel = { isSubtitleStylePanelOpened.value = false },
            )
        }

        AnimatedVisibility(
            visible =  isSyncSubtitlesPanelOpened.value,
            enter = bottomHalfSlideEnter,
            exit = bottomHalfSlideExit,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SubtitleSyncPanel(
                hidePanel = { isSyncSubtitlesPanelOpened.value = false },
            )
        }

        AnimatedVisibility(
            visible =  isAudioAndSubtitlesPanelOpened.value,
            enter = fadeIn() + slideInHorizontally { it / 2 },
            exit = fadeOut() + slideOutHorizontally { it / 2 },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            AudioAndSubtitlesPanel(
                hidePanel = { isAudioAndSubtitlesPanelOpened.value = false },
            )
        }

        AnimatedVisibility(
            visible =  isServerPanelOpened.value,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            ServersPanel(
                state = state,
                servers = servers,
                providers = providers,
                onProviderChange = onProviderChange,
                onServerChange = onServerChange,
                hidePanel = { isServerPanelOpened.value = false },
            )
        }

        AnimatedVisibility(
            visible =  isPlaybackSpeedPanelOpened.value,
            enter = bottomHalfSlideEnter,
            exit = bottomHalfSlideExit,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            PlaybackSpeedPanel(
                hidePanel = { isPlaybackSpeedPanelOpened.value = false }
            )
        }
    }
}

@Composable
private fun slideTransition(
    initialOffsetY: (fullHeight: Int) -> Int = { -it },
    targetOffsetY: (fullHeight: Int) -> Int = { -it },
): Pair<EnterTransition, ExitTransition> {
    return Pair(
        fadeIn() + slideInVertically(initialOffsetY = initialOffsetY),
        slideOutVertically(targetOffsetY = targetOffsetY) + fadeOut()
    )
}