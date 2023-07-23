package com.flixclusive.presentation.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.graphics.Color.BLACK
import android.graphics.Color.TRANSPARENT
import android.graphics.Color.WHITE
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Rational
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.flixclusive.domain.model.consumet.VideoData
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.utils.ConsumetUtils.getAutoSourceUrl
import com.flixclusive.presentation.common.Functions.isThereLessThan10SecondsLeftToWatch
import com.flixclusive.presentation.common.VideoDataDialogState
import com.flixclusive.presentation.film.dialog_content.VideoPlayerDialog
import com.flixclusive.presentation.player.controls.PlayerControls
import com.flixclusive.ui.theme.FlixclusiveTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

const val PLAYER_SEEK_BACK_INCREMENT = 5000L
const val PLAYER_SEEK_FORWARD_INCREMENT = 10000L
const val WATCH_HISTORY_ITEM = "watch_history_item"
const val SEASON_COUNT = "season_count"
const val EPISODE_SELECTED = "episode_selected"
const val VIDEO_DATA = "video_data"

@Suppress("DEPRECATION")
@AndroidEntryPoint
@UnstableApi
class PlayerActivity : ComponentActivity() {
    companion object {
        fun Context.startPlayer(
            videoData: VideoData?,
            watchHistoryItem: WatchHistoryItem? = null,
            seasonCount: Int? = null,
            episodeSelected: TMDBEpisode? = null,
        ) {
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(VIDEO_DATA, videoData)

                if(watchHistoryItem != null)
                    putExtra(WATCH_HISTORY_ITEM, watchHistoryItem)
                if(seasonCount != null)
                    putExtra(SEASON_COUNT, seasonCount)
                if(episodeSelected != null)
                    putExtra(EPISODE_SELECTED, episodeSelected)
            }
            this.startActivity(intent)
        }
    }

    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private lateinit var afChangeListener: AudioManager.OnAudioFocusChangeListener

    private var resumeOnFocusGain = false
    private var shouldPlay = true
    private var playbackDelayed = false
    private var playbackNowAuthorized = false
    private val focusLock = Any()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            FlixclusiveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel = hiltViewModel<PlayerViewModel>()
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    var onEpisodeClickJob: Job? by remember { mutableStateOf(null) }
                    var playerTimeUpdaterJob: Job? by remember { mutableStateOf(null) }

                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
                    val seasonData by viewModel.season.collectAsStateWithLifecycle()
                    val videoData by viewModel.videoData.collectAsStateWithLifecycle()
                    val currentSelectedEpisode by viewModel.currentSelectedEpisode.collectAsStateWithLifecycle()

                    var autoAdaptiveSource by remember { mutableStateOf(videoData.getAutoSourceUrl()) }
                    val availableQualities = remember(viewModel.availableQualities.size) { viewModel.availableQualities }
                    val subtitlesList = remember(videoData.subtitles.first().url) { viewModel.availableSubtitles }

                    var mediaSession: MediaSession? by remember {
                        mutableStateOf(
                            initializePlayer(
                                autoAdaptiveSource,
                                videoData.title,
                                subtitlesList,
                                uiState.currentTime,
                                uiState.playWhenReady
                            )
                        )
                    }

                    // Re-prepare the player if video data has changed
                    LaunchedEffect(videoData.headers.referer) {
                        val currentAutoAdaptiveSource = videoData.getAutoSourceUrl()
                        val isNew = !currentAutoAdaptiveSource.equals(autoAdaptiveSource, ignoreCase = true)

                        if(isNew) {
                            autoAdaptiveSource = currentAutoAdaptiveSource
                            mediaSession?.player?.rePrepare(
                                autoAdaptiveSource,
                                videoData,
                                subtitlesList,
                                uiState.currentTime,
                                uiState.playWhenReady
                            )
                        }
                    }

                    var shouldShowControls by remember { mutableStateOf(true) }
                    val snackbarBottomPadding by animateDpAsState(
                        targetValue = if(shouldShowControls) 100.dp else 0.dp
                    )

                    val isLastEpisode = remember(currentSelectedEpisode) {
                        val watchHistoryItem = viewModel.watchHistoryItem

                        val lastSeason = watchHistoryItem.seasons
                        val lastEpisode = watchHistoryItem.episodes[lastSeason]

                        currentSelectedEpisode?.season == lastSeason && currentSelectedEpisode?.episode == lastEpisode
                    }

                    fun initializePlayer(playerListener: Player.Listener) {
                        if(mediaSession != null) {
                            mediaSession!!.player.addListener(playerListener)
                            return
                        }

                        mediaSession = initializePlayer(
                            autoAdaptiveSource,
                            videoData.title,
                            subtitlesList,
                            uiState.currentTime,
                            uiState.playWhenReady
                        )

                        mediaSession?.player?.addListener(playerListener)
                    }
                    fun releasePlayer() {
                        mediaSession?.run {
                            viewModel.onActivityStop(
                                playWhenReady = player.isPlaying && player.playWhenReady,
                                currentTime = player.currentPosition
                            )
                            viewModel.updateWatchHistory()

                            //player.removeListener(playerListener)
                            player.release()
                            release()
                            mediaSession = null
                        }
                    }
                    fun onEpisodeClick(episode: TMDBEpisode? = null) {
                        if(onEpisodeClickJob?.isActive == true)
                            return

                        onEpisodeClickJob = scope.launch {
                            viewModel.onEpisodeClick(episodeToWatch = episode)
                        }
                    }

                    InitializeAudioFocusChangeListener(
                        play = {
                            mediaSession?.player?.play()
                            viewModel.updateIsPlayingState(true)
                        },
                        pause = {
                            mediaSession?.player?.pause()
                            viewModel.updateIsPlayingState(false)
                        }
                    )
                    RequestAudioFocus(
                        isPlaying = uiState.isPlaying,
                        playbackState = uiState.playbackState,
                        onRequestGranted = {
                            if(playerTimeUpdaterJob?.isActive == true)
                                return@RequestAudioFocus

                            playerTimeUpdaterJob = scope.launch {
                                mediaSession?.player?.play()
                                viewModel.updateIsPlayingState(true)

                                while (mediaSession?.player?.isPlaying == true) {
                                    viewModel.updateCurrentTime(mediaSession?.player?.currentPosition)
                                    delay(1.seconds / 30)
                                }
                            }
                        }
                    )
                    LifecycleAwarePlayer(
                        onEventCallback = { duration, currentPosition, bufferPercentage, isPlaying, playbackState ->
                            viewModel.updatePlayerState(
                                totalDuration = duration,
                                currentTime = currentPosition,
                                bufferedPercentage = bufferPercentage,
                                isPlaying = isPlaying,
                                playbackState = playbackState
                            )
                        },
                        onPlaybackReady = {
                            mediaSession?.player?.run {
                                viewModel.initializeVideoQualities(currentTracks)
                                viewModel.updateWatchHistory()
                                trackSelectionParameters =
                                    viewModel.onSubtitleChange(
                                        subtitleIndex = uiState.selectedSubtitle,
                                        trackParameters = trackSelectionParameters
                                    )
                            }
                        },
                        onPlaybackIdle = {
                            // Re-prepare the media player.
                            mediaSession?.player?.run {
                                prepare()
                                playWhenReady = uiState.playWhenReady
                            }
                        },
                        onPlaybackEnded = {
                            if(!isLastEpisode)
                                onEpisodeClick()
                        },
                        onInitialize = { initializePlayer(it) },
                        onReleasePlayer = { releasePlayer() }
                    )
                    LaunchedEffect(uiState.currentTime, uiState.isPlaying, uiState.totalDuration) {
                        val isThereLessThan10SecondsLeft = isThereLessThan10SecondsLeftToWatch(uiState.currentTime, uiState.totalDuration)
                        val isPlayerInitialized = uiState.isPlaying && uiState.totalDuration > 0L
                        val isTvShow = viewModel.seasonCount != null

                        if(isPlayerInitialized && isThereLessThan10SecondsLeft && !isLastEpisode && isTvShow) {
                            val secondsLeft = (uiState.totalDuration - uiState.currentTime) / 1000

                            if(secondsLeft <= 0L) {
                                viewModel.showSnackbar(
                                    message = "Loading next episode...",
                                    type = PlayerSnackbarMessageType.Episode
                                )
                                return@LaunchedEffect
                            }

                            viewModel.showSnackbar(
                                message = "Next episode on $secondsLeft...",
                                type = PlayerSnackbarMessageType.Episode
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                shouldShowControls = !shouldShowControls
                            }
                    ) {
                        AndroidView(
                            factory = {
                                PlayerView(context).apply {
                                    useController = false
                                    init()
                                }
                            },
                            update = {
                                it.run {
                                    player = mediaSession?.player
                                    if (SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
                                        init(isInPictureInPictureMode = true)
                                    }
                                }
                            }
                        )

                        PlayerControls(
                            watchHistoryItem = viewModel.watchHistoryItem,
                            dialogState = dialogState,
                            seasonDataProvider = { seasonData },
                            availableSeasons = viewModel.seasonCount,
                            currentEpisodeSelected = currentSelectedEpisode,
                            isLastEpisode = isLastEpisode,
                            subtitlesProvider = { videoData.subtitles },
                            videoServersProvider = { videoData.servers!! },
                            videoQualitiesProvider = { availableQualities },
                            shouldShowControls = {
                                if (SDK_INT >= Build.VERSION_CODES.N) {
                                    shouldShowControls &&
                                    !isInPictureInPictureMode
                                } else {
                                    shouldShowControls
                                }
                            },
                            showControls = { shouldShowControls = it },
                            onBack = { this@PlayerActivity.finish() },
                            isPlaying = { uiState.isPlaying },
                            title = { (mediaSession?.player?.mediaMetadata?.displayTitle ?: "").toString() },
                            onReplayClick = { mediaSession?.player?.seekBack() },
                            onForwardClick = { mediaSession?.player?.seekForward() },
                            onPauseToggle = {
                                when {
                                    mediaSession?.player?.isPlaying == true -> {
                                        // pause the video
                                        mediaSession!!.player.pause()
                                    }
                                    mediaSession?.player?.isPlaying == false && uiState.playbackState == STATE_ENDED -> {
                                        mediaSession!!.player.seekTo(0)
                                        mediaSession!!.player.playWhenReady = true
                                    }
                                    else -> {
                                        // play the video
                                        // it's already paused
                                        mediaSession?.player?.play()
                                    }
                                }

                                viewModel.updateIsPlayingState()
                                shouldPlay = uiState.isPlaying
                            },
                            totalDuration = { uiState.totalDuration },
                            currentTime = { uiState.currentTime },
                            bufferedPercentage = { uiState.bufferedPercentage },
                            playbackState = { uiState.playbackState },
                            selectedSubtitleProvider = { uiState.selectedSubtitle },
                            selectedVideoQualityProvider = { uiState.selectedQuality },
                            selectedVideoServerProvider = { uiState.selectedServer },
                            onSeekChanged = { timeMs: Float ->
                                mediaSession?.player?.seekTo(timeMs.toLong())
                            },
                            onSeekBack = {
                                mediaSession?.player?.seekBack()
                            },
                            onSeekForward = {
                                mediaSession?.player?.seekForward()
                            },
                            onSnackbarToggle = viewModel::showSnackbar,
                            onSeasonChange = {
                                viewModel.onSeasonChange(it)
                            },
                            onSubtitleChange = { subtitleIndex ->
                                mediaSession?.player?.run {
                                    trackSelectionParameters =
                                        viewModel.onSubtitleChange(
                                            subtitleIndex = subtitleIndex,
                                            trackParameters = trackSelectionParameters
                                        )
                                }
                            },
                            onVideoQualityChange = { videoQualityIndex ->
                                mediaSession?.player?.run {
                                    trackSelectionParameters =
                                        viewModel.onVideoQualityChange(
                                            qualityIndex = videoQualityIndex,
                                            trackParameters = trackSelectionParameters
                                        ) ?: return@run
                                }
                            },
                            onVideoServerChange = {
                                viewModel.onVideoServerChange(serverIndex = it)
                            },
                            onEpisodeClick = { onEpisodeClick(it) }
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .widthIn(min = 250.dp, max = 465.dp)
                                .heightIn(min = 65.dp, max = 195.dp)
                                .padding(bottom = snackbarBottomPadding)
                        ) {
                            LazyColumn {
                                itemsIndexed(viewModel.snackbarQueue) { i, data ->
                                    PlayerSnackbarVisuals(
                                        messageData = data,
                                        onDismissMessage = {
                                            viewModel.removeSnackbar(i)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if(dialogState != VideoDataDialogState.IDLE) {
                        if(mediaSession?.player?.isPlaying == true) {
                            mediaSession?.player?.pause()
                        }

                        VideoPlayerDialog(
                            videoDataDialogState = dialogState,
                            onConsumeDialog = {
                                // If something error has happened,
                                // we will go back to the tv show details screen
                                if (dialogState == VideoDataDialogState.ERROR || dialogState == VideoDataDialogState.UNAVAILABLE) {
                                    this@PlayerActivity.finish()
                                }

                                viewModel.onConsumePlayerDialog()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                with(PictureInPictureParams.Builder()) {
                    val width = 16
                    val height = 9
                    setAspectRatio(Rational(width, height))
                    build()
                }
            )
        }
    }

    @Composable
    private fun InitializeAudioFocusChangeListener(
        play: () -> Unit,
        pause: () -> Unit
    ) {
        LaunchedEffect(Unit) {
            afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (playbackDelayed || resumeOnFocusGain) {
                            synchronized(focusLock) {
                                playbackDelayed = false
                                resumeOnFocusGain = false
                            }
                            play()
                        }
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        synchronized(focusLock) {
                            resumeOnFocusGain = false
                            playbackDelayed = false
                        }
                        pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        synchronized(focusLock) {
                            // only resume if playback is being interrupted
                            resumeOnFocusGain = shouldPlay
                            playbackDelayed = false
                        }
                        pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        synchronized(focusLock) {
                            // only resume if playback is being interrupted
                            resumeOnFocusGain = shouldPlay
                            playbackDelayed = false
                        }
                        pause()
                    }
                }
            }
        }
    }

    @Composable
    private fun RequestAudioFocus(
        isPlaying: Boolean,
        playbackState: Int,
        onRequestGranted: () -> Unit
    ) {
        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        LaunchedEffect(isPlaying) {
            if(isPlaying) {
                val result = if (SDK_INT >= Build.VERSION_CODES.O) {
                    val playbackAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()

                    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(afChangeListener)
                        .build()

                    audioManager.requestAudioFocus(focusRequest)
                } else {
                    audioManager.requestAudioFocus(
                        afChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN
                    )
                }

                synchronized(focusLock) {
                    playbackNowAuthorized = when(result) {
                        AudioManager.AUDIOFOCUS_REQUEST_FAILED -> false
                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                            onRequestGranted()
                            shouldPlay = true
                            true
                        }
                        AudioManager.AUDIOFOCUS_REQUEST_DELAYED  -> {
                            playbackDelayed = true
                            false
                        }
                        else -> false
                    }
                }
            }
        }

        LaunchedEffect(isPlaying, playbackState) {
            if(isPlaying || playbackState == STATE_BUFFERING) {
                window?.addFlags(keepScreenOnFlag)
            } else {
                window?.clearFlags(keepScreenOnFlag)
            }
        }
    }
    
    private fun initializePlayer(
        url: String,
        title: String?,
        subtitles: List<SubtitleConfiguration>,
        playbackPosition: Long,
        playOnReadyState: Boolean
    ): MediaSession {
        val trackSelector = DefaultTrackSelector(this)
        val parameters = trackSelector
            .buildUponParameters()
            .setPreferredAudioLanguage(null)
            .build()

        trackSelector.parameters = parameters
        val player = ExoPlayer.Builder(this)
            .apply {
                setSeekBackIncrementMs(PLAYER_SEEK_BACK_INCREMENT)
                setSeekForwardIncrementMs(PLAYER_SEEK_FORWARD_INCREMENT)
                setTrackSelector(trackSelector)
            }
            .build()


        return MediaSession
            .Builder(this, player)
            .build()
            .apply {
                player.setMediaItem(
                    MediaItem.Builder()
                        .apply {
                            setUri(url)
                            setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setDisplayTitle(title)
                                    .build()
                            )
                            setSubtitleConfigurations(subtitles)
                        }
                        .build(),
                    playbackPosition
                )
                player.setHandleAudioBecomingNoisy(true)
                player.prepare()
                player.playWhenReady = playOnReadyState
            }
    }

    private fun Player.rePrepare(
        autoAdaptiveSource: String,
        videoData: VideoData,
        subtitles: List<SubtitleConfiguration>,
        startPositionMs: Long = 0L,
        playWhenReady: Boolean = true
    ) {
        setMediaItem(
            MediaItem.Builder()
                .apply {
                    setUri(autoAdaptiveSource)
                    setMediaMetadata(
                        MediaMetadata.Builder()
                            .setDisplayTitle(videoData.title)
                            .build()
                    )
                    setSubtitleConfigurations(subtitles)
                }
                .build(),
            startPositionMs
        )
        prepare()
        this.playWhenReady = playWhenReady
    }

    private fun PlayerView.init(isInPictureInPictureMode: Boolean = false) {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Add margin on subtitle view
        // Convert PX to DP for an adaptive margin
        val subtitleMarginBottom = when(isInPictureInPictureMode) {
            true -> 100
            false -> 200
        }

        val marginBottomInDp = (subtitleMarginBottom / resources.displayMetrics.density).toInt()
        val layoutParams = subtitleView?.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.setMargins(
            layoutParams.leftMargin,
            layoutParams.topMargin,
            layoutParams.rightMargin,
            marginBottomInDp,
        )
        subtitleView?.layoutParams = layoutParams

        // Modify subtitle style
        val style = CaptionStyleCompat(
            WHITE,
            TRANSPARENT,
            TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
            BLACK,
            Typeface.DEFAULT_BOLD
        )
        subtitleView?.setStyle(style)
        if(isInPictureInPictureMode) {
            subtitleView?.setApplyEmbeddedFontSizes(false)
            subtitleView?.setFixedTextSize(COMPLEX_UNIT_PX, 16F)
        }
    }
}