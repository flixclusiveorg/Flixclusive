package com.flixclusive.presentation.tv.screens.film.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.tv.material3.MaterialTheme
import com.flixclusive.common.LoggerUtils.debugLog
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.presentation.mobile.common.composables.GradientCircularProgressIndicator
import com.flixclusive.presentation.tv.main.TVMainActivity
import com.flixclusive.presentation.tv.utils.PlayerTvUtils.getTimeToSeekToBasedOnSeekMultiplier
import com.flixclusive.presentation.utils.ModifierUtils.handleDPadKeyEvents
import com.flixclusive.presentation.utils.PlayerUiUtils.LifecycleAwarePlayer
import com.flixclusive.presentation.utils.PlayerUiUtils.LocalPlayer
import com.flixclusive.presentation.utils.PlayerUiUtils.PLAYER_CONTROL_VISIBILITY_TIMEOUT
import com.flixclusive.presentation.utils.PlayerUiUtils.initializePlayer
import com.flixclusive.presentation.utils.PlayerUiUtils.rePrepare
import kotlinx.coroutines.delay

@UnstableApi
@Composable
fun FilmTvPlayerScreen(
    film: Film,
    isPlayerStarting: Boolean,
    episode: TMDBEpisode? = null,
    onBack: () -> Unit
) {
    val viewModel = hiltViewModel<TvPlayerViewModel>()

    val context = LocalContext.current as TVMainActivity

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
    val videoData by viewModel.videoData.collectAsStateWithLifecycle()
    val watchHistoryItem by viewModel.watchHistoryItem.collectAsStateWithLifecycle()
    val currentSelectedEpisode by viewModel.currentSelectedEpisode.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.play(film, episode)
    }

    AnimatedVisibility(
        visible = (dialogState is VideoDataDialogState.Fetching || dialogState is VideoDataDialogState.Extracting) && isPlayerStarting,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        GradientCircularProgressIndicator(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
            )
        )
    }

    if (dialogState is VideoDataDialogState.Success && isPlayerStarting) {
        LaunchedEffect(Unit) {
            if (film is TvShow) {
                viewModel.initializeWatchItemManager(film.totalSeasons)
            }
        }

        var controlTimeoutVisibility by remember { mutableIntStateOf(
            PLAYER_CONTROL_VISIBILITY_TIMEOUT
        ) }
        val areControlsVisible = remember(controlTimeoutVisibility) {
            controlTimeoutVisibility > 0
        }
        val snackbarBottomPadding by animateDpAsState(
            targetValue = if (areControlsVisible) 100.dp else 0.dp,
            label = ""
        )
        var seekMultiplier by remember { mutableLongStateOf(0L) }

        val isLastEpisode = remember(currentSelectedEpisode) {
            val lastSeason = watchHistoryItem?.seasons
            val lastEpisode = watchHistoryItem?.episodes?.get(lastSeason)

            currentSelectedEpisode?.season == lastSeason && currentSelectedEpisode?.episode == lastEpisode
        }
        var source by remember { mutableStateOf(videoData!!.source) }
        val availableQualities =
            remember(viewModel.availableQualities.size) { viewModel.availableQualities }
        var shouldPlay by remember { mutableStateOf(true) }
        val subtitlesList =
            remember(videoData!!.subtitles.first().url) { viewModel.availableSubtitles }

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
            controlTimeoutVisibility = if(isShowing) PLAYER_CONTROL_VISIBILITY_TIMEOUT else 0
        }

        LaunchedEffect(key1 = uiState, block = {
            debugLog("uiState = $uiState")
        })

        LaunchedEffect(uiState.isPlaying) {
            controlTimeoutVisibility = if(!uiState.isPlaying) {
               Int.MAX_VALUE
            } else PLAYER_CONTROL_VISIBILITY_TIMEOUT
        }

        LaunchedEffect(seekMultiplier) {
            if(seekMultiplier != 0L) {
                val timeToSeekTo = getTimeToSeekToBasedOnSeekMultiplier(
                    currentTime = uiState.currentTime,
                    maxDuration = uiState.totalDuration,
                    seekMultiplier = seekMultiplier
                )

                delay(1000L)
                mediaSession?.player?.seekTo(timeToSeekTo)
                seekMultiplier = 0
            }
        }

        LaunchedEffect(controlTimeoutVisibility) {
            if (controlTimeoutVisibility > 0) {
                delay(1000)
                controlTimeoutVisibility -= 1
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
                            viewModel.initializeVideoQualities(currentTracks)
                            viewModel.updateWatchHistory()

                            trackSelectionParameters = viewModel.onSubtitleChange(
                                subtitleIndex = uiState.selectedSubtitle,
                                trackParameters = trackSelectionParameters
                            )
                            trackSelectionParameters = viewModel.onVideoQualityChange(
                                qualityIndex = uiState.selectedQuality,
                                trackParameters = trackSelectionParameters
                            )
                        }
                    },
                    onPlaybackEnded = {
                        if (!isLastEpisode && watchHistoryItem?.film?.filmType == FilmType.TV_SHOW) {
                            viewModel.play(film)
                        }
                    },
                    onInitialize = {
                        if(mediaSession != null) {
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
                                if (!areControlsVisible) {
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
                        .focusable()
                )

                TvPlaybackControls(
                    modifier = Modifier
                        .fillMaxSize(),
                    isVisible = areControlsVisible && seekMultiplier == 0L,
                    stateProvider = { uiState },
                    dialogStateProvider = { dialogState },
                    playbackTitle = videoData?.title ?: "",
                    isTvShow = watchHistoryItem?.film?.filmType == FilmType.TV_SHOW,
                    isLastEpisode = isLastEpisode,
                    seekMultiplier = seekMultiplier,
                    showControls = { showControls(it) },
                    onSeekMultiplierChange = { seekMultiplier = it },
                    onPauseToggle = {
                        viewModel.updateIsPlayingState()
                        shouldPlay = uiState.isPlaying
                    },
                    onBack = onBack,
                    onNextEpisode = {
                        viewModel.play(film)
                    }
                )
            }
        }
    }
}


