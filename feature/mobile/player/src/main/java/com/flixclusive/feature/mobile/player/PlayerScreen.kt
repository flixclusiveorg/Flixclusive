package com.flixclusive.feature.mobile.player

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.ui.common.navigation.navigator.PlayerScreenNavigator
import com.flixclusive.core.ui.common.util.noIndicationClickable
import com.flixclusive.core.ui.mobile.ListenKeyEvents
import com.flixclusive.core.ui.mobile.rememberPipMode
import com.flixclusive.core.ui.mobile.util.toggleSystemBars
import com.flixclusive.core.ui.player.PLAYER_CONTROL_VISIBILITY_TIMEOUT
import com.flixclusive.core.ui.player.PlayerScreenNavArgs
import com.flixclusive.core.ui.player.PlayerSnackbarMessageType
import com.flixclusive.core.ui.player.util.ACTION_PIP_CONTROL
import com.flixclusive.core.ui.player.util.PLAYER_PIP_EVENT
import com.flixclusive.core.ui.player.util.PlayerUiUtil.AudioFocusManager
import com.flixclusive.core.ui.player.util.PlayerUiUtil.LifecycleAwarePlayer
import com.flixclusive.core.ui.player.util.PlayerUiUtil.LocalPlayerManager
import com.flixclusive.core.ui.player.util.PlayerUiUtil.ObserveNewLinksAndSubtitles
import com.flixclusive.core.ui.player.util.PlayerUiUtil.ObservePlayerTime
import com.flixclusive.core.ui.player.util.PlayerUiUtil.formatPlayerTitle
import com.flixclusive.core.ui.player.util.updatePiPParams
import com.flixclusive.core.util.android.getActivity
import com.flixclusive.domain.provider.CachedLinks
import com.flixclusive.feature.mobile.player.controls.PlayerControls
import com.flixclusive.feature.mobile.player.controls.dialogs.provider.ProviderResourceStateScreen
import com.flixclusive.feature.mobile.player.util.BrightnessManager
import com.flixclusive.feature.mobile.player.util.LocalBrightnessManager
import com.flixclusive.feature.mobile.player.util.PlayerPipReceiver
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(UnstableApi::class)
@Destination(navArgsDelegate = PlayerScreenNavArgs::class)
@Composable
internal fun PlayerScreen(
    navigator: PlayerScreenNavigator,
    args: PlayerScreenNavArgs,
) {
    val viewModel: PlayerScreenViewModel = hiltViewModel()

    val isInPipMode by rememberPipMode()

    val context = LocalContext.current.getActivity<ComponentActivity>()
    val brightnessManager = remember { BrightnessManager(context) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val providerState by viewModel.dialogState.collectAsStateWithLifecycle()

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val watchHistoryItem by viewModel.watchHistoryItem.collectAsStateWithLifecycle()

    val mediaData = viewModel.cachedLinks
    val providers by viewModel.providers.collectAsStateWithLifecycle(initialValue = emptyList())
    val seasonData by viewModel.season.collectAsStateWithLifecycle()
    val currentSelectedEpisode by viewModel.currentSelectedEpisode.collectAsStateWithLifecycle()

    var scrapingJob by remember { mutableStateOf<Job?>(null) }

    val currentPlayerTitle = remember(currentSelectedEpisode) {
        formatPlayerTitle(args.film, currentSelectedEpisode)
    }
    var controlTimeoutVisibility by remember {
        mutableIntStateOf(PLAYER_CONTROL_VISIBILITY_TIMEOUT)
    }
    val isEpisodesSheetOpened = remember { mutableStateOf(false) }
    val isAudiosAndSubtitlesDialogOpened = remember { mutableStateOf(false) }
    val isPlayerSettingsDialogOpened = remember { mutableStateOf(false) }
    val isServersDialogOpened = remember { mutableStateOf(false) }
    val isDoubleTapping = remember { mutableStateOf(false) }

    var isChangingEpisode by remember { mutableStateOf(false) }

    val snackbarBottomPadding by animateDpAsState(
        targetValue = if (viewModel.areControlsVisible && !viewModel.areControlsLocked) 100.dp else 0.dp,
        label = ""
    )

    fun showControls(isShowing: Boolean) {
        val areSomeSheetsOpened = isEpisodesSheetOpened.value
                || isPlayerSettingsDialogOpened.value
                || isAudiosAndSubtitlesDialogOpened.value
                || isServersDialogOpened.value
                || isDoubleTapping.value

        val isLoading = (!viewModel.player.hasBeenInitialized
                || !viewModel.player.isPlaying
                || viewModel.player.playbackState == Player.STATE_BUFFERING
                || viewModel.player.playbackState == Player.STATE_ENDED)
                && !viewModel.areControlsLocked

        controlTimeoutVisibility = when {
            !isShowing || areSomeSheetsOpened -> 0
            isLoading -> Int.MAX_VALUE
            else -> PLAYER_CONTROL_VISIBILITY_TIMEOUT
        }
    }

    fun onEpisodeClick(episode: Episode? = null) {
        viewModel.onEpisodeClick(
            episodeToWatch = episode
        )
    }

    LaunchedEffect(currentSelectedEpisode) {
        if(currentSelectedEpisode != null && currentSelectedEpisode != args.episodeToPlay) {
            isChangingEpisode = true
            navigator.onEpisodeChange(
                film = args.film,
                episodeToPlay = currentSelectedEpisode!!
            )
        }
    }

    /**
     *
     * If a user comes back from an [Lifecycle.Event.ON_DESTROY] state for a long time,
     * the system will decide to kill the app. This side effect will
     * ensures that there will always be a [CachedLinks] to play for the player. Otherwise,
     * it's just going to fail to play and possibly throw an unhandled exception.
     *
     * */
    LaunchedEffect(Unit) {
        if (mediaData.watchId.isEmpty() || mediaData.providerName.isEmpty()) {
            when(args.film) {
                is TvShow -> onEpisodeClick(args.episodeToPlay)
                is Movie -> viewModel.loadMediaLinks()
                else -> throw IllegalStateException("Invalid film instance [${args.film.filmType}]: ${args.film}")
            }
        }
    }

    CompositionLocalProvider(
        LocalPlayerManager provides viewModel.player,
        LocalBrightnessManager provides brightnessManager
    ) {
        PlayerPipReceiver(
            action = ACTION_PIP_CONTROL,
            onReceive = { broadcastIntent ->
                if (
                    SDK_INT >= Build.VERSION_CODES.O
                    && broadcastIntent?.action == ACTION_PIP_CONTROL
                ) {
                    val event = broadcastIntent.getIntExtra(PLAYER_PIP_EVENT, -1)

                    if (event == -1)
                        return@PlayerPipReceiver

                    viewModel.player.handleBroadcastEvents(event)
                }
            }
        )

        /**
         * Check if activity is forced to go to
         * pip mode, then update the ui to pip view
         * based on the current player ui state
         * */
        LaunchedEffect(
            isInPipMode,
            viewModel.player.isPlaying,
            viewModel.player.playbackState
        ) {
            if (SDK_INT >= Build.VERSION_CODES.O && isInPipMode) {
                context.updatePiPParams(
                    isPlaying = viewModel.player.isPlaying,
                    hasEnded = viewModel.player.playbackState == Player.STATE_ENDED,
                    preferredSeekIncrement = appSettings.preferredSeekAmount
                )
            }
        }

        LaunchedEffect(viewModel.player.playbackState) {
            when (viewModel.player.playbackState) {
                Player.STATE_READY -> {
                    // Find film's audio language
                    viewModel.run {
                        if (!player.hasBeenInitialized) {
                            player.onAudioChange(language = film.language ?: return@run)
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    if (!viewModel.isLastEpisode && !isInPipMode) {
                        viewModel.player.run {
                            viewModel.updateWatchHistory(
                                currentTime = currentPosition,
                                duration = duration
                            )
                        }
                        onEpisodeClick()
                    }
                }
            }
        }

        /**
         *
         * Initialize every view state for the player
         * and put them back on the default state if
         * the user is leaving out of the player
         * */
        DisposableEffect(LocalLifecycleOwner.current) {
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            context.toggleSystemBars(isVisible = false)

            // Initialize brightness
            val userDefaultBrightnessLevel = brightnessManager.currentBrightness

            onDispose {
                // Lock the screen to landscape only if we're just changing
                // episodes. This is really bad code bruh.
                if (!isChangingEpisode) {
                    context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    brightnessManager.setBrightness(userDefaultBrightnessLevel)
                    context.toggleSystemBars(isVisible = true)
                }

                isChangingEpisode = false
            }
        }

        ListenKeyEvents { code, _ ->
            when (code) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    viewModel.player.volumeManager.increaseVolume()
                    true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    viewModel.player.volumeManager.decreaseVolume()
                    true
                }
                else -> false
            }
        }

        LaunchedEffect(controlTimeoutVisibility) {
            if (controlTimeoutVisibility > 0) {
                viewModel.areControlsVisible = true
                delay(1000)
                controlTimeoutVisibility -= 1
            } else viewModel.areControlsVisible = false
        }

        // Re-prepare the player if provider data has changed
        ObserveNewLinksAndSubtitles(
            selectedSourceLink = uiState.selectedSourceLink,
            currentPlayerTitle = currentPlayerTitle,
            newLinks = mediaData.streams,
            newSubtitles = mediaData.subtitles,
            getSavedTimeForCurrentSourceData = {
                viewModel.getSavedTimeForSourceData(currentSelectedEpisode).first
            }
        )

        /**
         *
         * Purpose (unless interacted):
         * Always show controls when player is paused.
         * Show it when player hasn't been initialized.
         * Don't show it if its locked and its buffering.
         * Show controls when buffering
         *
         * See local function (CTRL+F): showControls
         *
         * */
        LaunchedEffect(
            viewModel.player.hasBeenInitialized,
            viewModel.areControlsLocked,
            viewModel.player.isPlaying,
            viewModel.player.playbackState,
            isEpisodesSheetOpened.value,
            isPlayerSettingsDialogOpened.value,
            isAudiosAndSubtitlesDialogOpened.value,
            isServersDialogOpened.value,
            isDoubleTapping.value
        ) {
            showControls(true)
        }

        ObservePlayerTime(
            isTvShow = args.film.filmType == FilmType.TV_SHOW,
            isLastEpisode = viewModel.isLastEpisode,
            isInPipMode = isInPipMode,
            showSnackbar = viewModel::showSnackbar,
            onQueueNextEpisode = {
                viewModel.onQueueNextEpisode()
            }
        )

        AudioFocusManager(
            activity = context,
            preferredSeekAmount = appSettings.preferredSeekAmount,
            isPiPModeEnabled = appSettings.isPiPModeEnabled
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            LifecycleAwarePlayer(
                areControlsVisible = viewModel.areControlsVisible && !viewModel.areControlsLocked,
                isInPipMode = isInPipMode,
                resizeMode = uiState.selectedResizeMode,
                onInitialize = {
                    viewModel.run {
                        val (currentPosition, _)
                            = getSavedTimeForSourceData(currentSelectedEpisode)

                        player.initialize()
                        mediaData.run {
                            val getPossibleSourceLink = streams
                                .getOrNull(uiState.selectedSourceLink)
                                ?: streams.getOrNull(0)

                            getPossibleSourceLink?.let {
                                player.prepare(
                                    link = it,
                                    title = currentPlayerTitle,
                                    subtitles = subtitles,
                                    initialPlaybackPosition = currentPosition
                                )
                            }
                        }
                    }
                },
                onRelease = { isForceReleasing ->
                    viewModel.player.run {
                        viewModel.updateWatchHistory(
                            currentTime = currentPosition,
                            duration = duration
                        )

                        release(isForceReleasing = isForceReleasing)
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .noIndicationClickable {
                        showControls(!viewModel.areControlsVisible)
                    }
            )

            PlayerControls(
                isVisible = viewModel.areControlsVisible && !isInPipMode,
                appSettings = appSettings,
                areControlsLocked = viewModel.areControlsLocked,
                isDoubleTapping = isDoubleTapping,
                isEpisodesSheetOpened = isEpisodesSheetOpened,
                isAudiosAndSubtitlesDialogOpened = isAudiosAndSubtitlesDialogOpened,
                servers = mediaData.streams,
                isPlayerSettingsDialogOpened = isPlayerSettingsDialogOpened,
                isServersDialogOpened = isServersDialogOpened,
                watchHistoryItem = watchHistoryItem,
                providerApis = providers,
                availableSeasons = (args.film as? TvShow)?.totalSeasons,
                currentEpisodeSelected = currentSelectedEpisode,
                isLastEpisode = viewModel.isLastEpisode,
                state = uiState,
                seasonData = seasonData,
                onBack = navigator::goBack,
                onSnackbarToggle = viewModel::showSnackbar,
                onSeasonChange = viewModel::onSeasonChange,
                onVideoServerChange = viewModel::onServerChange,
                onProviderChange = { newProvider ->
                    viewModel.onProviderChange(
                        newProvider =  newProvider
                    )
                },
                onResizeModeChange = viewModel::onResizeModeChange,
                onPanelChange = viewModel::onPanelChange,
                toggleVideoTimeReverse = viewModel::toggleVideoTimeReverse,
                showControls = { showControls(it) },
                lockControls = { viewModel.areControlsLocked = it },
                addSubtitle = { mediaData.subtitles.add(index = 0, element = it) },
                onEpisodeClick = {
                    viewModel.run {
                        updateWatchHistory(
                            currentTime = player.currentPosition,
                            duration = player.duration
                        )

                        onEpisodeClick(it)
                    }
                },
            )

            if (!isInPipMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = snackbarBottomPadding)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth(0.55F),
                    ) {
                        items(
                            items = viewModel.snackbarQueue,
                            key = { data ->
                                if (data.type == PlayerSnackbarMessageType.Error) {
                                    data.type.ordinal + Random.nextInt()
                                } else data.type.ordinal
                            }
                        ) { data ->
                            PlayerSnackbar(
                                messageData = data,
                                onDismissMessage = {
                                    viewModel.removeSnackbar(data)
                                }
                            )
                        }
                    }
                }
            }
        }
    }


    AnimatedVisibility(
        visible = !providerState.isIdle,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        with(viewModel) {
            DisposableEffect(Unit) {
                if (player.isPlaying) {
                    player.pause()
                    player.playWhenReady = true
                }

                onDispose {
                    scrapingJob?.cancel()
                    scrapingJob = null
                }
            }

            ProviderResourceStateScreen(
                state = providerState,
                servers = mediaData.streams,
                onSkipLoading = {
                    updateWatchHistory(
                        currentTime = player.currentPosition,
                        duration = player.duration
                    )

                    onEpisodeClick()
                },
                onClose = {
                    scrapingJob?.cancel()
                    scrapingJob = null
                    onConsumePlayerDialog()

                    if (player.playWhenReady) {
                        player.play()
                    }
                }
            )
        }
    }
}