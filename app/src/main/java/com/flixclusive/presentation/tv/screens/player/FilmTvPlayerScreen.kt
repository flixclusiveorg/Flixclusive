package com.flixclusive.presentation.tv.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.R
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.Movie
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.presentation.mobile.common.composables.GradientCircularProgressIndicator
import com.flixclusive.presentation.tv.main.TVMainActivity
import com.flixclusive.presentation.tv.screens.player.controls.BottomControlsButtonType
import com.flixclusive.presentation.tv.screens.player.controls.TvPlaybackControls
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.provideLocalDirectionalFocusRequester
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.handleDPadKeyEvents
import com.flixclusive.presentation.tv.utils.PlayerTvUtils.getTimeToSeekToBasedOnSeekMultiplier
import com.flixclusive.presentation.utils.PlayerUiUtils.LifecycleAwarePlayer
import com.flixclusive.presentation.utils.PlayerUiUtils.LocalPlayer
import com.flixclusive.presentation.utils.PlayerUiUtils.PLAYER_CONTROL_VISIBILITY_TIMEOUT
import com.flixclusive.presentation.utils.PlayerUiUtils.initializePlayer
import com.flixclusive.presentation.utils.PlayerUiUtils.rePrepare
import kotlinx.coroutines.delay

private const val PLAYER_SCREEN_DELAY = 800

@UnstableApi
@Composable
fun FilmTvPlayerScreen(
    film: Film,
    isPlayerStarting: Boolean,
    episode: TMDBEpisode? = null,
    onBack: () -> Unit,
) {
    val viewModel = hiltViewModel<TvPlayerViewModel>()

    val context = LocalContext.current as TVMainActivity

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
    val videoData by viewModel.videoData.collectAsStateWithLifecycle()
    val watchHistoryItem by viewModel.watchHistoryItem.collectAsStateWithLifecycle()
    val currentSelectedEpisode by viewModel.currentSelectedEpisode.collectAsStateWithLifecycle()

    LaunchedEffect(episode, isPlayerStarting) {
        if (
            ((currentSelectedEpisode?.episodeId == episode?.episodeId
                    && film is TvShow) || film is Movie)
            && dialogState !is VideoDataDialogState.Error
            && dialogState !is VideoDataDialogState.Unavailable
            && dialogState !is VideoDataDialogState.Idle
        ) return@LaunchedEffect

        viewModel.play(film, episode)
    }

    BackHandler(
        enabled = dialogState !is VideoDataDialogState.Idle && isPlayerStarting
    ) {
        onBack()
    }

    AnimatedVisibility(
        visible = dialogState !is VideoDataDialogState.Idle && isPlayerStarting,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .sizeIn(
                        minHeight = 250.dp,
                        minWidth = 250.dp
                    )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    if (
                        dialogState is VideoDataDialogState.Extracting
                        || dialogState is VideoDataDialogState.Fetching
                        || dialogState is VideoDataDialogState.Success
                    ) {
                        GradientCircularProgressIndicator(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                            )
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.round_error_outline_24),
                            contentDescription = "Error icon",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(80.dp)
                        )
                    }

                    Text(
                        text = dialogState.message.asString(),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(15.dp)
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = dialogState is VideoDataDialogState.Success && isPlayerStarting && videoData != null,
        enter = fadeIn(animationSpec = tween(delayMillis = PLAYER_SCREEN_DELAY)),
        exit = fadeOut(animationSpec = tween(delayMillis = PLAYER_SCREEN_DELAY))
    ) {
        LaunchedEffect(Unit) {
            if (film is TvShow) {
                viewModel.initializeWatchItemManager(film.totalSeasons)
            }
        }

        var controlTimeoutVisibility by remember {
            mutableIntStateOf(PLAYER_CONTROL_VISIBILITY_TIMEOUT)
        }
        val (sideSheetFocusPriority, toggleSideSheet) = remember {
            mutableStateOf<BottomControlsButtonType?>(
                null
            )
        }
        var areControlsVisible by remember { mutableStateOf(true) }
        var seekMultiplier by remember { mutableLongStateOf(0L) }

        val playerFocusRequester = remember { FocusRequester() }

        val isLastEpisode = remember(currentSelectedEpisode) {
            val lastSeason = watchHistoryItem?.seasons
            val lastEpisode = watchHistoryItem?.episodes?.get(lastSeason)

            currentSelectedEpisode?.season == lastSeason && currentSelectedEpisode?.episode == lastEpisode
        }
        var source by remember { mutableStateOf(videoData!!.source) }
        var shouldPlay by remember { mutableStateOf(true) }
        val availableQualities =
            remember(viewModel.availableQualities.size) { viewModel.availableQualities }
        val subtitlesList =
            remember(videoData?.subtitles?.first()?.url) { viewModel.availableSubtitles }

        var mediaSession: MediaSession? by remember {
            mutableStateOf(
                context.initializePlayer(
                    source,
                    videoData?.title,
                    subtitlesList,
                    uiState.currentTime,
                    uiState.playWhenReady
                )
            )
        }

        fun showControls(isShowing: Boolean) {
            controlTimeoutVisibility = if (isShowing) PLAYER_CONTROL_VISIBILITY_TIMEOUT else 0
        }

        LaunchedEffect(uiState.isPlaying, uiState.playbackState) {
            if(sideSheetFocusPriority != null) {
                controlTimeoutVisibility = 0
                return@LaunchedEffect
            }

            controlTimeoutVisibility = if (
                (!uiState.isPlaying || uiState.playbackState == Player.STATE_BUFFERING)
            ) {
                Int.MAX_VALUE
            } else PLAYER_CONTROL_VISIBILITY_TIMEOUT
        }

        LaunchedEffect(seekMultiplier) {
            if (seekMultiplier != 0L) {
                var shouldPlayAfterSeek = false
                if(mediaSession?.player?.isPlaying == true) {
                    mediaSession?.player?.pause()
                    shouldPlayAfterSeek = true
                }
                showControls(true)
                delay(2000L)

                val timeToSeekTo = getTimeToSeekToBasedOnSeekMultiplier(
                    currentTime = uiState.currentTime,
                    maxDuration = uiState.totalDuration,
                    seekMultiplier = seekMultiplier
                )
                mediaSession?.player?.seekTo(timeToSeekTo)
                seekMultiplier = 0

                if(shouldPlayAfterSeek)
                    mediaSession?.player?.play()
            }
        }

        LaunchedEffect(controlTimeoutVisibility) {
            if (controlTimeoutVisibility > 0) {
                areControlsVisible = true
                delay(1000)
                controlTimeoutVisibility -= 1
            } else {
                areControlsVisible = false

                if (sideSheetFocusPriority == null)
                    playerFocusRequester.requestFocus()
            }
        }

        LaunchedEffect(videoData?.source) {
            val currentAutoAdaptiveSource = videoData?.source
            val isNew = !currentAutoAdaptiveSource.equals(source, ignoreCase = true)

            if (isNew) {
                source = currentAutoAdaptiveSource!!
                mediaSession?.player?.rePrepare(
                    source,
                    videoData!!,
                    subtitlesList,
                    uiState.currentTime,
                    uiState.playWhenReady
                )
            }
        }

        CompositionLocalProvider(LocalPlayer provides mediaSession?.player) {
            provideLocalDirectionalFocusRequester {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    AudioFocusManager(
                        isPlaying = uiState.isPlaying,
                        shouldPlay = shouldPlay,
                        playbackState = uiState.playbackState,
                        onUpdateIsPlayingState = viewModel::updateIsPlayingState,
                        onUpdateCurrentTime = viewModel::updateCurrentTime,
                        onShouldPlayChange = { shouldPlay = it }
                    )

                    LifecycleAwarePlayer(
                        isInTv = true,
                        appSettings = appSettings,
                        areControlsVisible = areControlsVisible,
                        onEventCallback = { duration, currentPosition, bufferPercentage, isPlaying, playbackState ->
                            viewModel.updatePlayerState(
                                totalDuration = duration,
                                currentTime = currentPosition,
                                bufferedPercentage = bufferPercentage,
                                isPlaying = isPlaying,
                                playbackState = playbackState
                            )
                        },
                        playWhenReady = uiState.playWhenReady,
                        onPlaybackReady = {
                            mediaSession?.player?.run {
                                viewModel.updateIsPlayingState(playWhenReady)
                                playbackParameters =
                                    PlaybackParameters(uiState.playbackSpeed)

                                viewModel.initializeVideoQualities(currentTracks)
                                viewModel.updateWatchHistory()

                                trackSelectionParameters = viewModel.onSubtitleChange(
                                    subtitleIndex = uiState.selectedSubtitle,
                                    trackParameters = trackSelectionParameters
                                )
                                trackSelectionParameters = viewModel.onVideoQualityChange(
                                    qualityIndex = uiState.selectedQuality,
                                    trackParameters = trackSelectionParameters
                                ) ?: return@run
                            }
                        },
                        onPlaybackEnded = {
                            if (!isLastEpisode && watchHistoryItem?.film?.filmType == FilmType.TV_SHOW) {
                                viewModel.play(film)
                            }
                        },
                        onInitialize = {
                            if (mediaSession != null) {
                                mediaSession!!.player.addListener(it)
                                return@LifecycleAwarePlayer
                            }

                            mediaSession = context.initializePlayer(
                                source,
                                videoData?.title,
                                subtitlesList,
                                uiState.currentTime,
                                uiState.playWhenReady
                            )
                            mediaSession!!.player.addListener(it)
                        },
                        onRelease = {
                            mediaSession?.run {
                                viewModel.onActivityStop(
                                    playWhenReady = player.isPlaying && player.playWhenReady,
                                    currentTime = player.currentPosition
                                )
                                viewModel.updateWatchHistory()

                                player.removeListener(it)
                                player.release()
                                release()
                                mediaSession = null
                            }
                        },
                        modifier = Modifier
                            .handleDPadKeyEvents(
                                onEnter = {
                                    if (!areControlsVisible && sideSheetFocusPriority == null) {
                                        showControls(true)
                                        mediaSession?.player?.run {
                                            if (isPlaying) {
                                                pause()
                                            } else play()
                                        }
                                    }
                                },
                                onUp = {
                                    showControls(true)
                                },
                                onDown = {
                                    showControls(true)
                                },
                                onLeft = {
                                    seekMultiplier -= 1
                                },
                                onRight = {
                                    seekMultiplier += 1
                                },
                            )
                            .focusRequester(playerFocusRequester)
                            .focusable()
                    )

                    TvPlaybackControls(
                        modifier = Modifier.fillMaxSize(),
                        isVisible = areControlsVisible && seekMultiplier == 0L && sideSheetFocusPriority == null,
                        qualities = availableQualities,
                        servers = videoData?.servers ?: emptyList(),
                        subtitles = videoData?.subtitles ?: emptyList(),
                        sideSheetFocusPriority = sideSheetFocusPriority,
                        stateProvider = { uiState },
                        dialogStateProvider = { dialogState },
                        playbackTitle = videoData?.title ?: "",
                        isTvShow = film.filmType == FilmType.TV_SHOW,
                        isLastEpisode = isLastEpisode,
                        seekMultiplier = seekMultiplier,
                        onSideSheetDismiss = { toggleSideSheet(it) },
                        showControls = { showControls(it) },
                        onSeekMultiplierChange = { seekMultiplier = it },
                        onPauseToggle = {
                            viewModel.updateIsPlayingState()
                            shouldPlay = uiState.isPlaying
                        },
                        onBack = {
                            viewModel.updateWatchHistory()
                            onBack()
                        },
                        onNextEpisode = {
                            viewModel.updateWatchHistory()
                            viewModel.play(film)
                        },
                        onSubtitleChange = { subtitleIndex, trackSelectionParameters ->
                            viewModel.onSubtitleChange(
                                subtitleIndex = subtitleIndex,
                                trackParameters = trackSelectionParameters
                            )
                        },
                        onVideoQualityChange = { videoQualityIndex, trackSelectionParameters ->
                            viewModel.onVideoQualityChange(
                                qualityIndex = videoQualityIndex,
                                trackParameters = trackSelectionParameters
                            )
                        },
                    )
                }
            }
        }
    }
}


