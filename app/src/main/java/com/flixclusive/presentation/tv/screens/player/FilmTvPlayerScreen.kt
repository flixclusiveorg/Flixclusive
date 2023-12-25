package com.flixclusive.presentation.tv.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.MaterialTheme
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.Movie
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.presentation.common.composables.SourceStateDialog
import com.flixclusive.presentation.common.player.FlixclusivePlayer
import com.flixclusive.presentation.common.player.PLAYER_CONTROL_VISIBILITY_TIMEOUT
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.LifecycleAwarePlayer
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.LocalPlayer
import com.flixclusive.presentation.tv.main.TVMainActivity
import com.flixclusive.presentation.tv.screens.player.controls.BottomControlsButtonType
import com.flixclusive.presentation.tv.screens.player.controls.TvPlaybackControls
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.provideLocalDirectionalFocusRequester
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.handleDPadKeyEvents
import com.flixclusive.presentation.tv.utils.PlayerTvUtils.getTimeToSeekToBasedOnSeekMultiplier
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

    SourceStateDialog(
        state = dialogState,
        isTv = true,
        onConsumeDialog = viewModel::onConsumePlayerDialog
    )

    AnimatedVisibility(
        visible = dialogState is VideoDataDialogState.Success && isPlayerStarting,
        enter = fadeIn(animationSpec = tween(delayMillis = PLAYER_SCREEN_DELAY)),
        exit = fadeOut(animationSpec = tween(delayMillis = PLAYER_SCREEN_DELAY))
    ) {
        LaunchedEffect(Unit) {
            viewModel.resetUiState()

            if (film is TvShow) {
                viewModel.initializeWatchItemManager(film.totalSeasons)
            }
        }

        var controlTimeoutVisibility by remember {
            mutableIntStateOf(PLAYER_CONTROL_VISIBILITY_TIMEOUT)
        }
        val (sideSheetFocusPriority, toggleSideSheet) = remember {
            mutableStateOf<BottomControlsButtonType?>(null)
        }
        var areControlsVisible by remember { mutableStateOf(true) }
        var seekMultiplier by remember { mutableLongStateOf(0L) }

        val playerFocusRequester = remember { FocusRequester() }

        val isLastEpisode = remember(currentSelectedEpisode) {
            val lastSeason = watchHistoryItem?.seasons
            val lastEpisode = watchHistoryItem?.episodes?.get(lastSeason)

            currentSelectedEpisode?.season == lastSeason && currentSelectedEpisode?.episode == lastEpisode
        }
        var source by remember { mutableStateOf(videoData.source) }

        val player by remember { 
            mutableStateOf(FlixclusivePlayer(context, appSettings))
        }

        fun showControls(isShowing: Boolean) {
            controlTimeoutVisibility = if (isShowing) PLAYER_CONTROL_VISIBILITY_TIMEOUT else 0
        }

        LaunchedEffect(player.isPlaying, player.playbackState) {
            if(sideSheetFocusPriority != null) {
                controlTimeoutVisibility = 0
                return@LaunchedEffect
            }

            controlTimeoutVisibility = if (
                (!player.isPlaying || player.playbackState == Player.STATE_BUFFERING)
            ) {
                Int.MAX_VALUE
            } else PLAYER_CONTROL_VISIBILITY_TIMEOUT
        }

        LaunchedEffect(seekMultiplier) {
            if (seekMultiplier != 0L) {
                var shouldPlayAfterSeek = false

                if(player.isPlaying) {
                    player.pause()
                    shouldPlayAfterSeek = true
                }
                showControls(true)
                delay(2000L)

                val timeToSeekTo = getTimeToSeekToBasedOnSeekMultiplier(
                    currentTime = player.currentPosition,
                    maxDuration = player.duration,
                    seekMultiplier = seekMultiplier
                )
                player.seekTo(timeToSeekTo)
                seekMultiplier = 0

                if(shouldPlayAfterSeek)
                    player.play()
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

        LaunchedEffect(videoData.source) {
            val currentAutoAdaptiveSource = videoData.source
            val isNew = !currentAutoAdaptiveSource.equals(source, ignoreCase = true)

            if (isNew) {
                source = currentAutoAdaptiveSource

                val (currentPosition, _) = viewModel.getSavedTimeForVideoData(currentSelectedEpisode)

                player.prepare(
                    videoData = videoData,
                    initialPlaybackPosition = currentPosition
                )
            }
        }

        CompositionLocalProvider(LocalPlayer provides player) {
            provideLocalDirectionalFocusRequester {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    AudioFocusManager()

                    LifecycleAwarePlayer(
                        isInTv = true,
                        areControlsVisible = areControlsVisible,
                        onInitialize = {
                            player.run {
                                val (currentPosition, _) = viewModel.getSavedTimeForVideoData(currentSelectedEpisode)

                                initialize()
                                prepare(
                                    videoData = videoData,
                                    initialPlaybackPosition = currentPosition
                                )
                            }
                        },
                        onRelease = {
                            player.run {
                                viewModel.updateWatchHistory(
                                    currentTime = currentPosition,
                                    duration = duration
                                )

                                release()
                            }
                        },
                        modifier = Modifier
                            .handleDPadKeyEvents(
                                onEnter = {
                                    if (!areControlsVisible && sideSheetFocusPriority == null) {
                                        showControls(true)
                                        player.run {
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
                        videoData = videoData,
                        sideSheetFocusPriority = sideSheetFocusPriority,
                        stateProvider = { uiState },
                        dialogStateProvider = { dialogState },
                        playbackTitle = videoData.title ?: "",
                        isTvShow = film.filmType == FilmType.TV_SHOW,
                        isLastEpisode = isLastEpisode,
                        seekMultiplier = seekMultiplier,
                        onSideSheetDismiss = { toggleSideSheet(it) },
                        showControls = { showControls(it) },
                        onSeekMultiplierChange = { seekMultiplier = it },
                        onBack = {
                            player.run {
                                viewModel.updateWatchHistory(
                                    currentTime = currentPosition,
                                    duration = duration
                                )
                                onBack()
                            }
                        },
                        onNextEpisode = {
                            player.run {
                                viewModel.updateWatchHistory(
                                    currentTime = currentPosition,
                                    duration = duration
                                )
                                viewModel.play(film)
                            }
                        },
                    )
                }
            }
        }
    }
}


