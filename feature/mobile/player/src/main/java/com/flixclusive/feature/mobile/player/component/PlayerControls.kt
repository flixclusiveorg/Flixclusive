package com.flixclusive.feature.mobile.player.component

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.common.extensions.noIndicationClickable
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
import com.flixclusive.feature.mobile.player.component.bottom.BottomControls
import com.flixclusive.feature.mobile.player.component.episodes.EpisodesScreen
import com.flixclusive.feature.mobile.player.component.servers.ServersScreen
import com.flixclusive.feature.mobile.player.component.subtitles.SubtitleAndAudioScreen
import com.flixclusive.feature.mobile.player.component.top.PlayerTopBar
import com.flixclusive.feature.mobile.player.util.UiPanel
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.provider.ProviderMetadata


@OptIn(UnstableApi::class)
@Composable
internal fun PlayerControls(
    player: AppPlayer,
    film: FilmMetadata,
    playerPrefs: PlayerPreferences,
    subtitlesPrefs: SubtitlesPreferences,
    currentProvider: ProviderMetadata,
    providers: List<ProviderMetadata>,
    onProviderChange: (ProviderMetadata) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    currentEpisode: Episode? = null,
    currentSeason: SeasonWithProgress? = null,
    onSeasonChange: ((Season) -> Unit)? = null,
    onEpisodeChange: ((Episode) -> Unit)? = null,
    onNext: (() -> Unit)? = null
) {
    var isLocked by rememberSaveable { mutableStateOf(false) }
    var visiblePanel by rememberSaveable { mutableStateOf(UiPanel.NONE) }
    var queueControlVisibility by rememberSaveable { mutableStateOf(false) }

    val scrubState = rememberScrubState(player = player)
    val playPauseState = rememberPlayPauseButtonState(player = player)
    val seekButtonState = rememberSeekButtonState(player = player)
    val playbackSpeedState = rememberPlaybackSpeedState(player = player)
    val controlsVisibilityState = rememberControlsVisibilityState(
        player = player,
        isScrubbing = scrubState.event == ScrubEvent.SCRUBBING
    )

    val serversState = rememberServersState(player = player)
    val tracksState = rememberTracksState(
        player = player,
        subtitlesPreferences = subtitlesPrefs,
        playerPreferences = playerPrefs
    )

    LaunchedEffect(
        visiblePanel,
        controlsVisibilityState.isVisible
    ) {
        if (visiblePanel.isPlaybackSpeed) {
            controlsVisibilityState.show(indefinite = true)
            return@LaunchedEffect
        }

        if (controlsVisibilityState.isVisible && !visiblePanel.isNone) {
            controlsVisibilityState.hide()
            queueControlVisibility = true
        } else if (queueControlVisibility && visiblePanel.isNone) {
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
                        if (visiblePanel.isPlaybackSpeed) {
                            visiblePanel = UiPanel.NONE
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

                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    VerticalSlideAnimation(
                        visible = controlsVisibilityState.isVisible,
                        slideDown = false,
                    ) {
                        PlayerTopBar(
                            title = film.title,
                            episode = currentEpisode,
                            onBack = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }

                    VerticalSlideAnimation(
                        visible = controlsVisibilityState.isVisible,
                        slideDown = true,
                    ) {
                        BottomControls(
                            playPauseState = playPauseState,
                            seekButtonState = seekButtonState,
                            playbackSpeedState = playbackSpeedState,
                            scrubState = scrubState,
                            isSpeedPanelOpen = visiblePanel.isPlaybackSpeed,
                            onNext = onNext,
                            onLock = { isLocked = true },
                            onToggleUiPanel = { visiblePanel = it },
                            onShowEpisodesPanel = currentEpisode?.let {
                                { visiblePanel = UiPanel.EPISODES }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }

                }

                AnimatedPanel(visible = visiblePanel.isSubs) {
                    SubtitleAndAudioScreen(
                        tracksState = tracksState,
                        onDismiss = { visiblePanel = UiPanel.NONE },
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }

                AnimatedPanel(
                    visible = visiblePanel.isEpisodes
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
                        onDismiss = { visiblePanel = UiPanel.NONE },
                        modifier = Modifier
                            .fillMaxSize(),
                    )
                }

                AnimatedPanel(
                    visible = visiblePanel.isServers
                ) {
                    ServersScreen(
                        serversState = serversState,
                        onDismiss = { visiblePanel = UiPanel.NONE },
                        currentProvider = currentProvider,
                        providers = providers,
                        onProviderChange = onProviderChange,
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
