package com.flixclusive.feature.mobile.player.component

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.player.ResizeMode
import com.flixclusive.core.presentation.common.extensions.noIndicationClickable
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.presentation.player.ui.effect.AutoNextServerEffect
import com.flixclusive.core.presentation.player.ui.state.ControlsVisibilityState.Companion.rememberControlsVisibilityState
import com.flixclusive.core.presentation.player.ui.state.PlayPauseButtonState.Companion.rememberPlayPauseButtonState
import com.flixclusive.core.presentation.player.ui.state.PlaybackSpeedState.Companion.rememberPlaybackSpeedState
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState
import com.flixclusive.core.presentation.player.ui.state.ScrubState.Companion.rememberScrubState
import com.flixclusive.core.presentation.player.ui.state.SeekButtonState.Companion.rememberSeekButtonState
import com.flixclusive.core.presentation.player.ui.state.SeekPreviewState.Companion.rememberSeekPreviewState
import com.flixclusive.core.presentation.player.ui.state.TracksState.Companion.rememberTracksState
import com.flixclusive.core.presentation.player.ui.state.VolumeManager.Companion.rememberVolumeManager
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.feature.mobile.player.R
import com.flixclusive.feature.mobile.player.component.bottom.BottomControls
import com.flixclusive.feature.mobile.player.component.bottom.PlaybackSpeedSheet
import com.flixclusive.feature.mobile.player.component.bottom.ResizeModeSheet
import com.flixclusive.feature.mobile.player.component.center.CenterControls
import com.flixclusive.feature.mobile.player.component.effect.NextEpisodeCountdownEffect
import com.flixclusive.feature.mobile.player.component.episode.EpisodesScreen
import com.flixclusive.feature.mobile.player.component.gesture.BrightnessManager.Companion.rememberBrightnessManager
import com.flixclusive.feature.mobile.player.component.gesture.PlayerGestureHandler
import com.flixclusive.feature.mobile.player.component.gesture.PlayerGestureState.Companion.rememberPlayerGestureState
import com.flixclusive.feature.mobile.player.component.gesture.SpeedBoostIndicator
import com.flixclusive.feature.mobile.player.component.server.ServersScreen
import com.flixclusive.feature.mobile.player.component.snackbar.PlayerErrorSnackbar
import com.flixclusive.feature.mobile.player.component.snackbar.PlayerSnackbar
import com.flixclusive.feature.mobile.player.component.subtitle.SubtitleAndAudioScreen
import com.flixclusive.feature.mobile.player.component.subtitle.SubtitleSyncScreen
import com.flixclusive.feature.mobile.player.component.top.PlayerTopBar
import com.flixclusive.feature.mobile.player.util.UiMode
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.provider.ProviderMetadata
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.flixclusive.core.drawables.R as UiCommonR

@OptIn(UnstableApi::class)
@Composable
internal fun PlayerControls(
    player: AppPlayer,
    film: FilmMetadata,
    snackbarState: PlayerSnackbarState,
    isInPipMode: Boolean,
    playerPrefs: PlayerPreferences,
    subtitlesPrefs: SubtitlesPreferences,
    currentResizeMode: ResizeMode,
    currentProvider: ProviderMetadata,
    providers: List<ProviderMetadata>,
    servers: () -> List<PlayerServer>,
    failedStreamUrls: () -> Set<String>,
    currentServer: () -> Int,
    onServerChange: (Int) -> Unit,
    onServerFail: (Int) -> Unit,
    onProviderChange: (ProviderMetadata) -> Unit,
    onResizeModeChange: (ResizeMode) -> Unit,
    onBack: () -> Unit,
    currentSeason: () -> SeasonWithProgress?,
    onUpdateWatchProgress: () -> Unit,
    modifier: Modifier = Modifier,
    currentEpisode: Episode? = null,
    onSeasonChange: ((Season) -> Unit)? = null,
    onEpisodeChange: ((Episode) -> Unit)? = null,
    onNext: (() -> Unit)? = null,
) {
    var isLocked by remember { mutableStateOf(false) }
    var uiMode by remember { mutableStateOf(UiMode.NONE) }
    var queueControlVisibility by remember { mutableStateOf(false) }
    var queuePlay by remember { mutableStateOf(false) }
    var bottomControlsHeightPx by remember { mutableIntStateOf(0) }
    var savedSpeed by remember { mutableFloatStateOf(0f) }
    var volumeSliderHideJob by remember { mutableStateOf<Job?>(null) }
    val key = remember(currentEpisode, currentProvider) { currentEpisode?.id + currentProvider.id }

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val scrubState = rememberScrubState(player = player)

    val playPauseState = rememberPlayPauseButtonState(player = player)
    val seekButtonState = rememberSeekButtonState(player = player)
    val playbackSpeedState = rememberPlaybackSpeedState(player = player)
    val controlsVisibilityState = rememberControlsVisibilityState(
        player = player,
        isScrubbing = { scrubState.isScrubbing }
    )

    val volumeManager = rememberVolumeManager(player = player)
    val brightnessManager = rememberBrightnessManager()
    val gestureState = rememberPlayerGestureState(seekAmountMs = seekButtonState.seekForwardAmountMs)

    val areCenterControlsVisible by remember {
        derivedStateOf {
            controlsVisibilityState.isVisible
                && !uiMode.isPlaybackSpeed
                && !uiMode.isResize
                && !gestureState.isDoubleTapping
                && !gestureState.isSliding
                && !gestureState.isSpeedBoosting
                && !scrubState.isScrubbing
        }
    }

    val seekPreviewState = rememberSeekPreviewState(
        player = player,
        key = { key }
    )
    val tracksState = rememberTracksState(
        player = player,
        subtitlesPreferences = subtitlesPrefs,
        playerPreferences = playerPrefs
    )

    SideEffect {
        if (!scrubState.isScrubbing
            && !gestureState.isSliding
            && !gestureState.isDoubleTapping
            && !gestureState.isSpeedBoosting) {
            onUpdateWatchProgress()
        }
    }

    BackHandler(enabled = isLocked) {
        controlsVisibilityState.show()
    }

    AutoNextServerEffect(
        key = { key + currentServer },
        player = player,
        availableServers = servers,
        currentServer = currentServer,
        onServerChange = onServerChange,
        onServerFail = onServerFail,
        snackbarState = snackbarState
    )

    if (onNext != null) {
        NextEpisodeCountdownEffect(
            scrubState = scrubState,
            snackbarState = snackbarState,
            isPlaying = { !playPauseState.showPlay },
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AutoPipModeObserverForAndroidOToR(
            player = player,
            isInPipMode = isInPipMode,
            playPauseState = playPauseState,
            seekAmount = playerPrefs.seekAmount,
            onPipInvoke = {
                player.setSubtitleStyle(true)
                isLocked = false
                uiMode = UiMode.NONE
                queueControlVisibility = controlsVisibilityState.isVisible
                controlsVisibilityState.hide()
            }
        )
    }

    LaunchedEffect(
        isInPipMode,
        uiMode,
        controlsVisibilityState.isVisible,
        gestureState.isDoubleTapping,
        gestureState.isSliding,
        gestureState.isSpeedBoosting,
        scrubState.isScrubbing
    ) {
        if (isInPipMode) {
            controlsVisibilityState.hide()
            return@LaunchedEffect
        }

        if (uiMode.isPlaybackSpeed || uiMode.isResize || scrubState.isScrubbing) {
            queueControlVisibility = true // to reset indefinite timer
            controlsVisibilityState.show(indefinite = true)
            return@LaunchedEffect
        }

        val shouldHideControls = gestureState.isGestureActive || !uiMode.isNone
        if (controlsVisibilityState.isVisible && shouldHideControls) {
            queueControlVisibility = true
            controlsVisibilityState.hide()

            val shouldPause = !uiMode.isSubsSync && !uiMode.isSubs && !gestureState.isGestureActive
            if (player.isPlaying && shouldPause) {
                queuePlay = true
                player.playWhenReady = true
                player.pause()
            }

            return@LaunchedEffect
        }

        if (queueControlVisibility && !shouldHideControls) {
            queueControlVisibility = false
            controlsVisibilityState.show()

            if (queuePlay && !player.isPlaying) {
                queuePlay = false
                player.play()
            }
        }
    }

    LaunchedEffect(controlsVisibilityState.isVisible) {
        var subtitleBottomPaddingFraction = 0.05f
        if (controlsVisibilityState.isVisible && !isLocked) {
            subtitleBottomPaddingFraction = 0.20f
        }

        player.subtitleView?.setBottomPaddingFraction(subtitleBottomPaddingFraction)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(gestureState.isSpeedBoosting) {
        if (gestureState.isSpeedBoosting) {
            savedSpeed = playbackSpeedState.playbackSpeed
            playbackSpeedState.updatePlaybackSpeed(2f)
        } else if (savedSpeed > 0f) {
            playbackSpeedState.updatePlaybackSpeed(savedSpeed)
        }
    }

    LaunchedEffect(currentEpisode) {
        tracksState.resetTracks()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val handled = when (keyEvent.key) {
                    Key.VolumeUp -> { volumeManager.increaseVolume(); true }
                    Key.VolumeDown -> { volumeManager.decreaseVolume(); true }
                    else -> false
                }
                if (handled) {
                    gestureState.showVolumeSlider()
                    volumeSliderHideJob?.cancel()
                    volumeSliderHideJob = scope.launch {
                        delay(1000L)
                        gestureState.hideSliders()
                    }
                }
                handled
            }
    ) {
        ControlsBlackOverlay(
            visible = controlsVisibilityState.isVisible && !isLocked,
            modifier = Modifier.fillMaxSize()
        )

        PlayerSnackbarLayer(
            snackbarState = snackbarState,
            areControlsVisible = { controlsVisibilityState.isVisible },
            bottomControlsHeightPx = { bottomControlsHeightPx },
        )

        AnimatedContent(
            targetState = isLocked,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(),
                    initialContentExit = fadeOut()
                )
            },
        ) { state ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (state) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .noIndicationClickable {
                                controlsVisibilityState.toggle()
                            }
                    )

                    AnimatedVisibility(
                        visible = controlsVisibilityState.isVisible,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(durationMillis = 400))
                    ) {
                        LockControls(
                            unlock = { isLocked = false },
                            showControls = { controlsVisibilityState.show() }
                        )
                    }
                } else {
                    PlayerGestureHandler(
                        gestureState = gestureState,
                        brightnessManager = brightnessManager,
                        volumeManager = volumeManager,
                        areControlsVisible = controlsVisibilityState.isVisible,
                        onSeekForward = { player.seekForward() },
                        onSeekBackward = { player.seekBack() },
                        modifier = Modifier.fillMaxSize(),
                        onSingleTap = {
                            if (uiMode.isPlaybackSpeed || uiMode.isResize) {
                                uiMode = UiMode.NONE
                            } else {
                                controlsVisibilityState.toggle()
                            }
                        }
                    )

                    VerticalSlideAnimation(
                        visible = controlsVisibilityState.isVisible,
                        slideDown = false,
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        PlayerTopBar(
                            title = film.title,
                            episode = currentEpisode,
                            onBack = onBack,
                        )
                    }

                    AnimatedVisibility(
                        visible = areCenterControlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        CenterControls(
                            playPauseButtonState = playPauseState,
                            seekButtonState = seekButtonState,
                            modifier = Modifier
                        )
                    }

                    VerticalSlideAnimation(
                        visible = controlsVisibilityState.isVisible,
                        slideDown = true,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged { if (it.height > 0) bottomControlsHeightPx = it.height }
                    ) {
                        BottomControls(
                            playbackSpeedState = playbackSpeedState,
                            scrubState = scrubState,
                            seekPreviewState = seekPreviewState,
                            uiMode = uiMode,
                            onNext = onNext,
                            onToggleUiPanel = { uiMode = it },
                            onShowEpisodesPanel = currentEpisode?.let {
                                { uiMode = UiMode.EPISODES }
                            },
                        )
                    }

                    AnimatedVisibility(
                        visible = areCenterControlsVisible,
                        enter = slideInHorizontally { it / 4 } + fadeIn(),
                        exit = slideOutHorizontally { it / 6 } + fadeOut(),
                        modifier = Modifier
                            .padding(end = 30.dp)
                            .align(Alignment.CenterEnd)
                    ) {
                        PlainTooltipBox(
                            description = stringResource(R.string.lock),
                        ) {
                            IconButton(
                                onClick = { isLocked = true },
                                modifier = Modifier.background(
                                    Color.Black.copy(0.3f),
                                    shape = CircleShape
                                )
                            ) {
                                AdaptiveIcon(
                                    painter = painterResource(UiCommonR.drawable.lock_thin),
                                    contentDescription = stringResource(R.string.lock),
                                    dp = 30.dp
                                )
                            }
                        }
                    }

                    VerticalSlideAnimation(
                        slideDown = false,
                        visible = gestureState.isSpeedBoosting,
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        SpeedBoostIndicator(
                            modifier = Modifier
                                .padding(top = 24.dp)
                        )
                    }

                    AnimatedPanel(visible = uiMode.isSubs) {
                        SubtitleAndAudioScreen(
                            isSyncEnabled = player.currentCuesWithTiming.isNotEmpty(),
                            tracksState = tracksState,
                            onDismiss = { uiMode = UiMode.NONE },
                            onSyncSubtitles = { uiMode = UiMode.SUBS_SYNC },
                            pausePlayer = {
                                if (player.isPlaying) {
                                    player.playWhenReady = false
                                    player.pause()
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                        )
                    }

                    AnimatedPanel(
                        visible = uiMode.isEpisodes
                            && film is TvShow
                            && currentEpisode != null
                            && onEpisodeChange != null
                            && onSeasonChange != null
                    ) {
                        EpisodesScreen(
                            currentSeason = currentSeason,
                            seasons = (film as TvShow).seasons,
                            currentEpisode = currentEpisode!!,
                            onSeasonChange = onSeasonChange!!::invoke,
                            onEpisodeClick = onEpisodeChange!!::invoke,
                            onDismiss = { uiMode = UiMode.NONE },
                            modifier = Modifier
                                .fillMaxSize(),
                        )
                    }

                    AnimatedPanel(
                        visible = uiMode.isServers
                    ) {
                        ServersScreen(
                            servers = servers,
                            failedStreamUrls = failedStreamUrls,
                            currentServer = currentServer,
                            onServerChange = onServerChange,
                            currentProvider = currentProvider,
                            providers = providers,
                            onProviderChange = onProviderChange,
                            onDismiss = { uiMode = UiMode.NONE },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AnimatedPanel(
                        visible = uiMode.isSubsSync
                    ) {
                        SubtitleSyncScreen(
                            cuesWithTiming = player.currentCuesWithTiming,
                            currentOffset = player.offset,
                            scrubState = scrubState,
                            onBack = { uiMode = UiMode.SUBS },
                            onDismiss = { uiMode = UiMode.NONE },
                            onSave = {
                                player.changeSubtitleDelay(it)

                                // Force seek to update subtitle timings immediately after changing the offset
                                val isMediaSeekable = player.isCommandAvailable(
                                    command = Player.COMMAND_GET_CURRENT_MEDIA_ITEM
                                ) && player.isCurrentMediaItemSeekable

                                if (isMediaSeekable) {
                                    player.seekTo(scrubState.progress + 1L)
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                        )
                    }

                    AnimatedPanel(visible = uiMode == UiMode.PLAYBACK_SPEED) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset {
                                    IntOffset(x = 0, y = -bottomControlsHeightPx)
                                }
                        ) {
                            PlaybackSpeedSheet(
                                playbackSpeedState = playbackSpeedState,
                                onDismiss = { uiMode = UiMode.NONE },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }

                    AnimatedPanel(visible = uiMode == UiMode.RESIZE) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset {
                                    IntOffset(x = 0, y = -bottomControlsHeightPx)
                                }
                        ) {
                            ResizeModeSheet(
                                currentResizeMode = currentResizeMode,
                                onResizeModeChange = onResizeModeChange,
                                onDismiss = { uiMode = UiMode.NONE },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSnackbarLayer(
    snackbarState: PlayerSnackbarState,
    areControlsVisible: () -> Boolean,
    bottomControlsHeightPx: () -> Int,
) {
    val density = LocalDensity.current
    val snackbarBottomOffset by animateIntAsState(
        targetValue = if (areControlsVisible()) {
            bottomControlsHeightPx()
        } else {
            with(density) { 8.dp.roundToPx() }
        },
        label = "snackbar_bottom_offset",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset {
                IntOffset(x = 0, y = -snackbarBottomOffset)
            }
    ) {
        PlayerErrorSnackbar(
            snackbarState = snackbarState,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp)
        )

        PlayerSnackbar(
            snackbarState = snackbarState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp)
        )
    }
}

@Composable
private fun AnimatedPanel(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 4 },
        exit = fadeOut() + slideOutVertically { it / 6 },
        modifier = modifier,
        content = content
    )
}

@Composable
private fun ControlsBlackOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        VerticalSlideAnimation(
            visible = visible,
            slideDown = false,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black.copy(0.8f),
                                0.15f to Color.Transparent,
                            )
                        )
                    }
            )
        }

        VerticalSlideAnimation(
            visible = visible,
            slideDown = true,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.75f to Color.Transparent,
                                1f to Color.Black.copy(0.8f),
                            )
                        )
                    }
            )
        }
    }
}

@Composable
private fun VerticalSlideAnimation(
    visible: Boolean,
    slideDown: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { (it / 4) * (if (slideDown) 1 else -1) },
        exit = fadeOut() + slideOutVertically { (it / 6) * (if (slideDown) 1 else -1)},
        content = content,
        modifier = modifier,
    )
}
