package com.flixclusive.feature.mobile.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.util.noIndicationClickable
import com.flixclusive.core.ui.mobile.ListenKeyEvents
import com.flixclusive.core.ui.mobile.component.SourceDataDialog
import com.flixclusive.core.ui.mobile.rememberPipMode
import com.flixclusive.core.ui.mobile.util.toggleSystemBars
import com.flixclusive.core.ui.player.PLAYER_CONTROL_VISIBILITY_TIMEOUT
import com.flixclusive.core.ui.player.PlayerScreenNavArgs
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
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.feature.mobile.player.controls.PlayerControls
import com.flixclusive.feature.mobile.player.util.PlayerPipReceiver
import com.flixclusive.feature.mobile.player.util.percentOfVolume
import com.flixclusive.feature.mobile.player.util.setBrightness
import com.flixclusive.model.provider.SourceData
import com.flixclusive.model.provider.SourceDataState
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.model.tmdb.TvShow
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

interface PlayerScreenNavigator : GoBackAction {
    /**
     *
     * There will be cases where the system will kill
     * the app's process mercilessly if it's never put as an exclusion
     * in the **ignore battery optimization list**.
     * <br/>
     *
     * When the user comes back to the app after a long while,
     * it will scrape back the old data that were saved from [SavedStateHandle].
     * <br/>
     *
     * This action should trigger the `savedStateHandle` to
     * update its values based on [PlayerScreenNavArgs] - since that is where
     * the args are saved on.
     * <br/>
     *
     * So whenever the user comes back to the app, the last episode the user
     * was on would be re-used.
     * */
    fun onEpisodeChange(
        film: Film,
        episodeToPlay: TMDBEpisode
    )
}

@OptIn(UnstableApi::class)
@Destination(navArgsDelegate = PlayerScreenNavArgs::class)
@Composable
fun PlayerScreen(
    navigator: PlayerScreenNavigator,
    args: PlayerScreenNavArgs,
) {
    val viewModel: PlayerScreenViewModel = hiltViewModel()

    val isInPipMode by rememberPipMode()

    val context = LocalContext.current.getActivity<ComponentActivity>()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val watchHistoryItem by viewModel.watchHistoryItem.collectAsStateWithLifecycle()

    val sourceData = viewModel.sourceData
    val seasonData by viewModel.season.collectAsStateWithLifecycle()
    val currentSelectedEpisode by viewModel.currentSelectedEpisode.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

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

    var isChangingEpisode by remember { mutableStateOf(false) }

    val snackbarBottomPadding by animateDpAsState(
        targetValue = if (viewModel.areControlsVisible) 100.dp else 0.dp,
        label = ""
    )

    fun showControls(isShowing: Boolean) {
        val areSomeSheetsOpened = isEpisodesSheetOpened.value
                || isPlayerSettingsDialogOpened.value
                || isAudiosAndSubtitlesDialogOpened.value
                || isServersDialogOpened.value

        val isLoading = (!viewModel.player.hasBeenInitialized
                || !viewModel.player.isPlaying
                || viewModel.player.playbackState == Player.STATE_BUFFERING
                || viewModel.player.playbackState == Player.STATE_ENDED) && !viewModel.areControlsLocked

        controlTimeoutVisibility = if (!isShowing || areSomeSheetsOpened) {
            0
        } else if (isLoading) {
            Int.MAX_VALUE
        } else {
            PLAYER_CONTROL_VISIBILITY_TIMEOUT
        }
    }

    fun onEpisodeClick(episode: TMDBEpisode? = null) {
        viewModel.onEpisodeClick(
            episodeToWatch = episode,
            runWebView = viewModel::onRunWebView
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
     * ensures that there will always be a [SourceData] to play for the player. Otherwise,
     * it's just going to fail to play and possibly throw an unhandled exception.
     *
     * */
    LaunchedEffect(Unit) {
        if (sourceData.mediaId.isEmpty() || sourceData.providerName.isEmpty()) {
            when(args.film) {
                is TvShow -> onEpisodeClick(args.episodeToPlay)
                is Movie -> viewModel.loadSourceData(runWebView = viewModel::onRunWebView)
                else -> throw IllegalStateException("Invalid film instance [${args.film.filmType}]: ${args.film}")
            }
        }
    }

    CompositionLocalProvider(LocalPlayerManager provides viewModel.player) {
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
                            player.onAudioChange(language = film.language)
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
            val userDefaultBrightnessLevel = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                -1
            ) / 255F

            viewModel.updateScreenBrightness(userDefaultBrightnessLevel)

            // Initialize volume level
            viewModel.updateVolume(context.percentOfVolume())

            onDispose {
                // Lock the screen to landscape only if we're just changing
                // episodes. This is really bad code bruh.
                if (!isChangingEpisode) {
                    context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    context.setBrightness(userDefaultBrightnessLevel)
                    context.toggleSystemBars(isVisible = true)
                }

                isChangingEpisode = false
            }
        }

        ListenKeyEvents { code, _ ->
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            when (code) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    val newVolume = currentVolume + 1F

                    audioManager.setStreamVolume(
                        /* streamType = */ AudioManager.STREAM_MUSIC,
                        /* index = */ newVolume.roundToInt(),
                        /* flags = */ 0
                    )
                    viewModel.updateVolume((newVolume / maxVolume).coerceIn(0F, 1F))
                    true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val newVolume = currentVolume - 1F
                    audioManager.setStreamVolume(
                        /* streamType = */ AudioManager.STREAM_MUSIC,
                        /* index = */ newVolume.roundToInt(),
                        /* flags = */ 0
                    )
                    viewModel.updateVolume((newVolume / maxVolume).coerceIn(0F, 1F))
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
            newLinks = sourceData.cachedLinks,
            newSubtitles = sourceData.cachedSubtitles,
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
         * See local function (CTRL+F): [showControls]
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
        ) {
            showControls(true)
        }

        ObservePlayerTime(
            isTvShow = args.film.filmType == FilmType.TV_SHOW,
            isLastEpisode = viewModel.isLastEpisode,
            isInPipMode = isInPipMode,
            showSnackbar = viewModel::showSnackbar,
            onQueueNextEpisode = {
                viewModel.onQueueNextEpisode(runWebView = viewModel::onRunWebView)
            }
        )

        AudioFocusManager(
            activity = context,
            preferredSeekAmount = appSettings.preferredSeekAmount
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
                        val (currentPosition, _) = getSavedTimeForSourceData(
                            currentSelectedEpisode
                        )

                        player.initialize()
                        sourceData.run {
                            val getPossibleSourceLink = cachedLinks
                                .getOrNull(uiState.selectedSourceLink)
                                ?: cachedLinks.getOrNull(0)

                            getPossibleSourceLink?.let {
                                player.prepare(
                                    link = it,
                                    title = currentPlayerTitle,
                                    subtitles = cachedSubtitles,
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
                visibilityProvider = {
                    viewModel.areControlsVisible &&
                            !isInPipMode
                },
                appSettings = appSettings,
                areControlsLocked = viewModel.areControlsLocked,
                isEpisodesSheetOpened = isEpisodesSheetOpened,
                isAudiosAndSubtitlesDialogOpened = isAudiosAndSubtitlesDialogOpened,
                servers = sourceData.cachedLinks,
                isPlayerSettingsDialogOpened = isPlayerSettingsDialogOpened,
                isServersDialogOpened = isServersDialogOpened,
                watchHistoryItem = watchHistoryItem,
                providerApis = viewModel.sourceProviders,
                availableSeasons = (args.film as? TvShow)?.totalSeasons,
                currentEpisodeSelected = currentSelectedEpisode,
                isLastEpisode = viewModel.isLastEpisode,
                stateProvider = { uiState },
                seasonDataProvider = { seasonData },
                onBack = navigator::goBack,
                onSnackbarToggle = viewModel::showSnackbar,
                onSeasonChange = viewModel::onSeasonChange,
                onVideoServerChange = viewModel::onServerChange,
                onProviderChange = { newProvider ->
                    viewModel.onProviderChange(
                        newProvider =  newProvider,
                        runWebView = viewModel::onRunWebView
                    )
                },
                onResizeModeChange = viewModel::onResizeModeChange,
                onPanelChange = viewModel::onPanelChange,
                toggleVideoTimeReverse = viewModel::toggleVideoTimeReverse,
                showControls = { showControls(it) },
                toggleControlLock = { viewModel.areControlsLocked = it },
                onBrightnessChange = {
                    context.setBrightness(it)
                    viewModel.updateScreenBrightness(it)
                },
                onVolumeChange = {
                    viewModel.player.run {
                        val newVolume = (it * 15).roundToInt()

                        setDeviceVolume(newVolume)
                        viewModel.updateVolume(it)
                    }
                },
                addSubtitle = { sourceData.cachedSubtitles.add(index = 0, element = it) },
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
                            key = { data -> data.type }
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

    if (dialogState !is SourceDataState.Idle || viewModel.webView != null) {
        LaunchedEffect(Unit) {
            viewModel.player.run {
                if (isPlaying) {
                    pause()
                    playWhenReady = true
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center
        ) {
            if (dialogState !is SourceDataState.Idle) {
                SourceDataDialog(
                    state = dialogState,
                    onConsumeDialog = {
                        viewModel.onDestroyWebView()
                        viewModel.onConsumePlayerDialog()

                        if (viewModel.player.playWhenReady) {
                            viewModel.player.play()
                        }
                    }
                )
            }

            if (viewModel.webView != null) {
                AndroidView(
                    factory = { _ ->
                        viewModel.webView!!.also {
                            scope.launch {
                                val shouldPlay = viewModel.player.isPlaying
                                async { it.startScraping() }

                                delay(200)
                                if (shouldPlay) {
                                    viewModel.player.play()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .height(0.5.dp)
                        .width(0.5.dp)
                        .alpha(0F)
                )
            }
        }
    } else {
        viewModel.onDestroyWebView()
    }
}