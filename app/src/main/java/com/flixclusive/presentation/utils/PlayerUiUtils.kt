package com.flixclusive.presentation.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.CaptionSizePreference.Companion.getDp
import com.flixclusive.presentation.mobile.screens.player.PLAYER_SEEK_BACK_INCREMENT
import com.flixclusive.presentation.mobile.screens.player.PLAYER_SEEK_FORWARD_INCREMENT
import com.flixclusive_provider.models.common.VideoData


object PlayerUiUtils {
    const val PLAYER_CONTROL_VISIBILITY_TIMEOUT = 5
    const val SECONDS_TO_SEEK = 10000
    const val SECONDS_TO_SEEK_ON_STREAK1 = 30000
    const val SECONDS_TO_SEEK_ON_STREAK2 = 60000
    const val SECONDS_TO_SEEK_ON_STREAK3 = 300000

    private const val FONT_SIZE_PIP_MODE = 8F // Equivalent to 8dp


    val LocalPlayer = compositionLocalOf<Player?> { null }

    @UnstableApi
    fun PlayerView.init(
        appSettings: AppSettings,
        isInPictureInPictureMode: Boolean = false,
        isInTv: Boolean = false,
        areControlsVisible: Boolean,
    ) {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Add margin on subtitle view
        // Convert PX to DP for an adaptive margin
        var subtitleMarginBottom = when {
            isInPictureInPictureMode -> 70
            isInTv -> 210
            else -> 200
        }

        if(areControlsVisible && !isInTv) {
            subtitleMarginBottom += 360
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
            appSettings.subtitleColor,
            appSettings.subtitleBackgroundColor,
            Color.TRANSPARENT,
            appSettings.subtitleEdgeType.type,
            Color.BLACK,
            appSettings.subtitleFontStyle.typeface
        )

        val fontSize = if(isInPictureInPictureMode)
            FONT_SIZE_PIP_MODE
        else appSettings.subtitleSize.getDp(isInTv)

        subtitleView?.setApplyEmbeddedFontSizes(false)
        //subtitleView?.setApplyEmbeddedStyles(false)
        subtitleView?.setFixedTextSize(
            /* unit = */ COMPLEX_UNIT_SP,
            /* size = */ fontSize
        )
        subtitleView?.setStyle(style)
    }

    @UnstableApi
    fun Context.initializePlayer(
        url: String?,
        title: String?,
        subtitles: List<MediaItem.SubtitleConfiguration>,
        playbackPosition: Long,
        playOnReadyState: Boolean,
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
                player.setHandleAudioBecomingNoisy(true)
                url?.let {
                    player.setMediaItem(
                        MediaItem.Builder()
                            .apply {
                                setUri(it)
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
                    player.prepare()
                    player.playWhenReady = playOnReadyState
                }
            }
    }

    fun Player.rePrepare(
        url: String?,
        videoData: VideoData,
        subtitles: List<MediaItem.SubtitleConfiguration>,
        startPositionMs: Long = 0L,
        playWhenReady: Boolean = true,
    ) {
        url?.let {
            setMediaItem(
                MediaItem.Builder()
                    .apply {
                        setUri(url)
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
    }

    @SuppressLint("OpaqueUnitKey")
    @UnstableApi
    @Composable
    fun LifecycleAwarePlayer(
        modifier: Modifier = Modifier,
        isInPipModeProvider: () -> Boolean = { false },
        isInTv: Boolean = false,
        appSettings: AppSettings,
        areControlsVisible: Boolean,
        playWhenReady: Boolean,
        onEventCallback: (
            totalDuration: Long,
            currentTime: Long,
            bufferedPercentage: Int,
            isPlaying: Boolean,
            playbackState: Int,
        ) -> Unit,
        onPlaybackReady: () -> Unit,
        onPlaybackEnded: () -> Unit,
        onInitialize: (Player.Listener) -> Unit,
        onRelease: (Player.Listener) -> Unit,
    ) {
        val context = LocalContext.current
        val lifecycle by rememberUpdatedState(LocalLifecycleOwner.current.lifecycle)
        val player = LocalPlayer.current

        val isInPipMode by rememberUpdatedState(isInPipModeProvider())

        DisposableEffect(
            AndroidView(
                modifier = modifier,
                factory = {
                    PlayerView(context).apply {
                        useController = false
                        init(
                            appSettings = appSettings,
                            isInTv = isInTv,
                            isInPictureInPictureMode = isInPipMode,
                            areControlsVisible = areControlsVisible
                        )
                    }
                },
                update = {
                    it.init(
                        appSettings = appSettings,
                        isInPictureInPictureMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPipMode,
                        isInTv = isInTv,
                        areControlsVisible = areControlsVisible
                    )

                    it.player = player
                }
            )
        ) {
            val playerListener = object : Player.Listener {
                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) {
                    super.onEvents(player, events)
                    onEventCallback(
                        player.duration.coerceAtLeast(0L),
                        player.currentPosition.coerceAtLeast(0L),
                        player.bufferedPercentage,
                        player.isPlaying,
                        player.playbackState
                    )
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when(playbackState) {
                        Player.STATE_READY -> onPlaybackReady()
                        Player.STATE_ENDED -> onPlaybackEnded()
                        Player.STATE_IDLE -> {
                            player?.run {
                                prepare()
                                this.playWhenReady = playWhenReady
                            }
                        }
                        else -> Unit
                    }
                }
            }

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        onInitialize(playerListener)
                    }
                    Lifecycle.Event.ON_STOP -> {
                        onRelease(playerListener)
                    }
                    else -> Unit
                }
            }

            lifecycle.addObserver(observer)

            onDispose {
                if (isInTv) {
                    onRelease(playerListener)
                }
                lifecycle.removeObserver(observer)
            }
        }
    }
}