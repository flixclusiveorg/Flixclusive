package com.flixclusive.feature.mobile.player.component

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.flixclusive.core.presentation.player.ui.state.ControlsVisibilityState.Companion.rememberControlsVisibilityState
import com.flixclusive.core.presentation.player.ui.state.PlayPauseButtonState.Companion.rememberPlayPauseButtonState
import com.flixclusive.core.presentation.player.ui.state.PlaybackSpeedState.Companion.rememberPlaybackSpeedState
import com.flixclusive.core.presentation.player.ui.state.ScrubEvent
import com.flixclusive.core.presentation.player.ui.state.ScrubState.Companion.rememberScrubState
import com.flixclusive.core.presentation.player.ui.state.SeekButtonState.Companion.rememberSeekButtonState
import com.flixclusive.core.presentation.player.ui.state.ServersState.Companion.rememberServersState
import com.flixclusive.core.presentation.player.ui.state.TracksState.Companion.rememberTracksState
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.feature.mobile.player.R
import com.flixclusive.feature.mobile.player.component.bottom.BottomControls
import com.flixclusive.feature.mobile.player.component.center.CenterControls
import com.flixclusive.feature.mobile.player.component.episodes.EpisodesScreen
import com.flixclusive.feature.mobile.player.component.servers.ServersScreen
import com.flixclusive.feature.mobile.player.component.subtitles.SubtitleAndAudioScreen
import com.flixclusive.feature.mobile.player.component.subtitles.SubtitleSyncScreen
import com.flixclusive.feature.mobile.player.component.top.PlayerTopBar
import com.flixclusive.feature.mobile.player.util.UiMode
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.core.drawables.R as UiCommonR

@OptIn(UnstableApi::class)
@Composable
internal fun PlayerControls(
    player: AppPlayer,
    film: FilmMetadata,
    playerPrefs: PlayerPreferences,
    subtitlesPrefs: SubtitlesPreferences,
    currentResizeMode: ResizeMode,
    currentProvider: ProviderMetadata,
    providers: List<ProviderMetadata>,
    onProviderChange: (ProviderMetadata) -> Unit,
    onResizeModeChange: (ResizeMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    currentEpisode: Episode? = null,
    currentSeason: SeasonWithProgress? = null,
    onSeasonChange: ((Season) -> Unit)? = null,
    onEpisodeChange: ((Episode) -> Unit)? = null,
    onNext: (() -> Unit)? = null
) {
    var isLocked by rememberSaveable { mutableStateOf(false) }
    var uiMode by rememberSaveable { mutableStateOf(UiMode.NONE) }
    var queueControlVisibility by rememberSaveable { mutableStateOf(false) }

    val scrubState = rememberScrubState(player = player)
    val playPauseState = rememberPlayPauseButtonState(player = player)
    val seekButtonState = rememberSeekButtonState(player = player)
    val playbackSpeedState = rememberPlaybackSpeedState(player = player)
    val controlsVisibilityState = rememberControlsVisibilityState(
        player = player,
        isScrubbing = scrubState.event == ScrubEvent.SCRUBBING
    )

    val isCenterControlsVisible by remember {
        derivedStateOf {
            controlsVisibilityState.isVisible
                && !uiMode.isPlaybackSpeed
                && !uiMode.isResize
        }
    }

    val serversState = rememberServersState(player = player)
    val tracksState = rememberTracksState(
        player = player,
        subtitlesPreferences = subtitlesPrefs,
        playerPreferences = playerPrefs
    )

    LaunchedEffect(
        uiMode,
        controlsVisibilityState.isVisible
    ) {
        if (uiMode.isPlaybackSpeed || uiMode.isResize) {
            controlsVisibilityState.show(indefinite = true)
            return@LaunchedEffect
        }

        if (controlsVisibilityState.isVisible && !uiMode.isNone) {
            controlsVisibilityState.hide()
            queueControlVisibility = true
        } else if (queueControlVisibility && uiMode.isNone) {
            controlsVisibilityState.show()
            queueControlVisibility = false
        }
    }

    LaunchedEffect(controlsVisibilityState.isVisible) {
        var subtitleBottomPaddingFraction = 0.05f
        if (controlsVisibilityState.isVisible) {
            subtitleBottomPaddingFraction = 0.15f
        }

        player.subtitleView?.setBottomPaddingFraction(subtitleBottomPaddingFraction)
    }

    AnimatedContent(
        targetState = isLocked,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(),
                initialContentExit = fadeOut()
            )
        }
    ) { state ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .noIndicationClickable(
                    onClick = {
                        if (uiMode.isPlaybackSpeed || uiMode.isResize) {
                            uiMode = UiMode.NONE
                        } else {
                            controlsVisibilityState.toggle()
                        }
                    }
                )
        ) {
            if (state) {
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
                ControlsBlackOverlay(
                    visible = controlsVisibilityState.isVisible,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
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
                        visible = isCenterControlsVisible,
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
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        BottomControls(
                            playbackSpeedState = playbackSpeedState,
                            scrubState = scrubState,
                            uiMode = uiMode,
                            onNext = onNext,
                            onResizeModeChange = onResizeModeChange,
                            currentResizeMode = currentResizeMode,
                            onToggleUiPanel = { uiMode = it },
                            onShowEpisodesPanel = currentEpisode?.let {
                                { uiMode = UiMode.EPISODES }
                            },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isCenterControlsVisible,
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

                AnimatedPanel(visible = uiMode.isSubs) {
                    SubtitleAndAudioScreen(
                        tracksState = tracksState,
                        onDismiss = { uiMode = UiMode.NONE },
                        onSyncSubtitles = { uiMode = UiMode.SUBS_SYNC },
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }

                AnimatedPanel(
                    visible = uiMode.isEpisodes
                        && film is TvShow
                        && currentSeason != null
                        && currentEpisode != null
                        && onEpisodeChange != null
                        && onSeasonChange != null
                ) {
                    EpisodesScreen(
                        currentSeason = currentSeason!!,
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
                        serversState = serversState,
                        onDismiss = { uiMode = UiMode.NONE },
                        currentProvider = currentProvider,
                        providers = providers,
                        onProviderChange = onProviderChange,
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }

                AnimatedPanel(
                    visible = uiMode.isSubsSync
                ) {
                    SubtitleSyncScreen(
                        cues = player.currentCuesWithTiming,
                        currentOffset = player.offset,
                        currentPosition = scrubState.progress,
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
            }
        }
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
