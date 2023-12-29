package com.flixclusive.presentation.common.player.utils


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.flixclusive.R
import com.flixclusive.databinding.CustomPlayerBinding
import com.flixclusive.domain.model.SourceData
import com.flixclusive.domain.utils.WatchHistoryUtils
import com.flixclusive.presentation.common.player.FlixclusivePlayer
import com.flixclusive.presentation.mobile.screens.player.PlayerSnackbarMessageType
import com.flixclusive.presentation.mobile.screens.player.utils.PlayerPiPUtils.updatePiPParams
import com.flixclusive.providers.models.common.SourceLink
import com.flixclusive.providers.models.common.Subtitle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object PlayerComposeUtils {

    val LocalPlayer = compositionLocalOf<FlixclusivePlayer?> { null }

    @Composable
    fun rememberLocalPlayer(): FlixclusivePlayer {
        val player = LocalPlayer.current
        check(player != null) {
            stringResource(id = R.string.player_not_initialized)
        }

        return rememberUpdatedState(player).value
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("OpaqueUnitKey")
    @Composable
    fun LifecycleAwarePlayer(
        modifier: Modifier = Modifier,
        isInPipMode: Boolean = false,
        isInTv: Boolean = false,
        releaseOnStop: Boolean = true,
        resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
        areControlsVisible: Boolean,
        onInitialize: () -> Unit,
        onRelease: () -> Unit,
    ) {
        val lifecycle by rememberUpdatedState(LocalLifecycleOwner.current.lifecycle)
        val currentOnInitialize by rememberUpdatedState(onInitialize)
        val player = rememberLocalPlayer()

        DisposableEffect(
            AndroidViewBinding(
                modifier = modifier,
                factory = { inflater, viewGroup, attachToParent ->
                    val binding = CustomPlayerBinding.inflate(inflater, viewGroup, attachToParent)

                    binding.apply {
                        val subtitleViewHolder = root.findViewById<FrameLayout?>(R.id.subtitle_view_holder)

                        root.isClickable = false
                        root.isFocusable = false

                        playerView.run {
                            this@run.resizeMode = resizeMode
                            this@run.player = player.getPlayer()

                            // Show the controls forever
                            controllerShowTimeoutMs = Int.MAX_VALUE
                            showController()

                            // Move out the SubtitleView ouf ot the content frame
                            // To avoid zooming in when resizeMode == CENTER_CROP
                            (subtitleView?.parent as ViewGroup?)?.removeView(subtitleView)
                            subtitleViewHolder?.addView(subtitleView)
                        }
                    }

                    return@AndroidViewBinding binding
                }
            ) {
                playerView.run {
                    this.resizeMode = resizeMode
                    this.player = player.getPlayer()

                    if(!isControllerFullyVisible) {
                        showController()
                    }

                    player.setSubtitleStyle(
                        subtitleView = subtitleView,
                        isInPictureInPictureMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPipMode,
                        isInTv = isInTv,
                        areControlsVisible = areControlsVisible
                    )
                }
            }
        ) {
            // Pre-initialize the player
            if (player.getPlayer() == null) {
                currentOnInitialize()
            }

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        if (player.getPlayer() == null) {
                            currentOnInitialize()
                        } else if (player.playWhenReady) {
                            player.play()
                        }
                    }

                    Lifecycle.Event.ON_STOP -> {
                        player.playWhenReady = player.isPlaying
                        player.pause()

                        if (Build.VERSION.SDK_INT > 23 && releaseOnStop) {
                            onRelease()
                        }
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        if (Build.VERSION.SDK_INT <= 23 && releaseOnStop) {
                            onRelease()
                        }
                    }

                    else -> Unit
                }
            }

            lifecycle.addObserver(observer)

            onDispose {
                if(!isInPipMode) {
                    onRelease()
                }
                lifecycle.removeObserver(observer)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Composable
    fun AudioFocusManager(
        activity: Activity,
        preferredSeekAmount: Long,
    ) {
        val player = rememberLocalPlayer()
        val scope = rememberCoroutineScope()

        // Audio managers
        val audioManager = remember { activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        var afChangeListener: AudioManager.OnAudioFocusChangeListener? by remember { mutableStateOf(null) }
        var resumeOnFocusGain by rememberSaveable { mutableStateOf(false) }
        var playbackDelayed by rememberSaveable { mutableStateOf(false) }
        var playbackNowAuthorized by rememberSaveable { mutableStateOf(false) }
        var playerTimeUpdaterJob: Job? by remember { mutableStateOf(null) }
        val focusLock = remember { Any() }

        LaunchedEffect(Unit) {
            afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (playbackDelayed || resumeOnFocusGain) {
                            synchronized(focusLock) {
                                playbackDelayed = false
                                resumeOnFocusGain = false
                            }
                            player.play()
                            player.playWhenReady = true
                        }
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        synchronized(focusLock) {
                            // only resume if playback is being interrupted
                            resumeOnFocusGain = player.playWhenReady
                            playbackDelayed = false
                        }
                        player.pause()
                        player.playWhenReady = false
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        synchronized(focusLock) {
                            resumeOnFocusGain = false
                            playbackDelayed = false
                        }
                        player.pause()
                        player.playWhenReady = false
                    }
                }
            }
        }

        LaunchedEffect(player.isPlaying) {
            if(player.isPlaying) {
                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val playbackAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()

                    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(afChangeListener!!)
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
                            if (playerTimeUpdaterJob?.isActive == true)
                                return@LaunchedEffect

                            playerTimeUpdaterJob = scope.launch {
                                player.run {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        activity.updatePiPParams(
                                            isPlaying = isPlaying,
                                            hasEnded = playbackState == Player.STATE_ENDED,
                                            preferredSeekIncrement = preferredSeekAmount,
                                        )
                                    }

                                    observePlayerPosition()
                                }
                            }
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

        LaunchedEffect(player.isPlaying, player.playbackState) {
            val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

            if(player.isPlaying || player.playbackState == Player.STATE_BUFFERING) {
                activity.window?.addFlags(keepScreenOnFlag)
            } else {
                activity.window?.clearFlags(keepScreenOnFlag)
            }
        }
    }
    
    /**
     * 
     * Re-prepares the player if the provided [SourceData] has changed
     * 
     * */
    @Composable
    fun ObserveNewLinksAndSubtitles(
        selectedSourceLink: Int,
        currentPlayerTitle: String,
        newLinks: List<SourceLink>,
        newSubtitles: List<Subtitle>,
        getSavedTimeForCurrentSourceData: () -> Long
    ) {
        val player = rememberLocalPlayer()
        var currentSubtitlesSize by remember { mutableIntStateOf(newSubtitles.size) }
        var currentLoadedLink by remember { mutableStateOf(newLinks.getOrNull(selectedSourceLink)?.url) }
        
        val currentGetSavedTimeForCurrentSourceData by rememberUpdatedState(newValue = getSavedTimeForCurrentSourceData)

        LaunchedEffect(
            newLinks.getOrNull(0),
            player.availableSubtitles.size,
            player.hasBeenInitialized,
            selectedSourceLink
        ) {
            if (!player.hasBeenInitialized) {
                return@LaunchedEffect
            }

            val newLink = newLinks.getOrNull(selectedSourceLink)
            val isNewServer =
                newLink?.url?.equals(
                    currentLoadedLink,
                    ignoreCase = true
                ) == false

            /* plus one because of the 'off' subtitle */
            val hasNewSubtitle = currentSubtitlesSize + 1  < player.availableSubtitles.size

            if (isNewServer || hasNewSubtitle) {
                currentSubtitlesSize = player.availableSubtitles.size
                currentLoadedLink = newLink?.url

                player.prepare(
                    link = newLink!!,
                    title = currentPlayerTitle,
                    subtitles = newSubtitles,
                    initialPlaybackPosition = currentGetSavedTimeForCurrentSourceData()
                )
            }
        }
    }

    /**
     *
     * Observes the player current position/time.
     * This checks if the current player time is below
     * 10 seconds (if it is then show a loading next episode snackbar)
     * and 80% of the maximum time have been watched (if this is true
     * then proceed to queue up the next episode available).
     *
     * */
    @Composable
    fun ObservePlayerTimer(
        isTvShow: Boolean,
        isLastEpisode: Boolean,
        isInPipMode: Boolean,
        showSnackbar: (message: String, type: PlayerSnackbarMessageType) -> Unit,
        onQueueNextEpisode: () -> Unit,
    ) {
        val player = rememberLocalPlayer()
        
        LaunchedEffect(
            player.currentPosition,
            player.isPlaying,
            player.duration,
            isLastEpisode,
            isInPipMode
        ) {
            player.run {
                val areThereLessThan10SecondsLeft = WatchHistoryUtils.isTimeInRangeOfThreshold(
                    currentWatchTime = currentPosition,
                    totalDurationToWatch = duration,
                )

                val areThere20PercentLeft = WatchHistoryUtils.isTimeInRangeOfThreshold(
                    currentWatchTime = currentPosition,
                    totalDurationToWatch = duration,
                    threshold = WatchHistoryUtils.calculateRemainingAmount(
                        amount = duration,
                        percentage = 0.8
                    )
                )
                val isPlayerInitialized = isPlaying && duration > 0L

                if (
                    isPlayerInitialized
                    && !isLastEpisode
                    && isTvShow
                    && !isInPipMode
                ) {
                    if (
                        areThere20PercentLeft
                        && !areThereLessThan10SecondsLeft
                    ) {
                        onQueueNextEpisode()
                    }

                    if (areThereLessThan10SecondsLeft) {
                        val secondsLeft = (duration - currentPosition) / 1000

                        if (secondsLeft <= 0L) {
                            showSnackbar(
                                "Loading next episode...",
                                PlayerSnackbarMessageType.Episode
                            )
                            return@LaunchedEffect
                        }

                        showSnackbar(
                            "Next episode on $secondsLeft...",
                            PlayerSnackbarMessageType.Episode
                        )
                    }
                }
            }
        }
    }
}