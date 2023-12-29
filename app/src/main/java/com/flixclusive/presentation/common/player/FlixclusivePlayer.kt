package com.flixclusive.presentation.common.player

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.AppSettings.Companion.CaptionSizePreference.Companion.getDp
import com.flixclusive.domain.utils.FilmProviderUtils.addOffSubtitles
import com.flixclusive.presentation.common.player.renderer.CustomTextRenderer
import com.flixclusive.presentation.common.player.utils.PlayerBuilderUtils.disableSSLVerification
import com.flixclusive.presentation.common.player.utils.PlayerBuilderUtils.getCache
import com.flixclusive.presentation.common.player.utils.PlayerBuilderUtils.getLoadControl
import com.flixclusive.presentation.common.player.utils.PlayerBuilderUtils.getRenderers
import com.flixclusive.presentation.common.player.utils.SubtitleUtils.getIndexFromLanguage
import com.flixclusive.presentation.common.player.utils.SubtitleUtils.getSubtitleMimeType
import com.flixclusive.providers.models.common.SourceLink
import com.flixclusive.providers.models.common.Subtitle
import com.flixclusive.providers.utils.Constants.USER_AGENT
import com.flixclusive.utils.LoggerUtils.debugLog
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds


const val PLAYER_CONTROL_VISIBILITY_TIMEOUT = 5

/** toleranceBeforeUs – The maximum time that the actual position seeked to may precede the
 * requested seek position, in microseconds. Must be non-negative. */
private const val toleranceBeforeUs = 300_000L

/**
 * toleranceAfterUs – The maximum time that the actual position seeked to may exceed the requested
 * seek position, in microseconds. Must be non-negative.
 */
private const val toleranceAfterUs = 300_000L

private const val FONT_SIZE_PIP_MODE = 8F // Equivalent to 8dp

enum class PlayerEvents {
    PLAY,
    PAUSE,
    REPLAY,
    FORWARD,
    BACKWARD
}


/**
 *
 * A Player wrapper for [Player], [ExoPlayer] and [MediaSession]
 *
 */
@OptIn(UnstableApi::class)
class FlixclusivePlayer(
    private val context: Context,
    private val appSettings: AppSettings,
) : Player.Listener {
    private var mediaSession: MediaSession? by mutableStateOf(null)
    var hasBeenInitialized by mutableStateOf(false)
        private set

    var duration by mutableLongStateOf(0L)
        private set
    var currentPosition by mutableLongStateOf(0L)
        private set
    var bufferedPercentage by mutableIntStateOf(0)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var playbackState by mutableIntStateOf(Player.STATE_IDLE)
        private set

    var playbackSpeed by mutableFloatStateOf(1F)
        private set
    var selectedAudio by mutableIntStateOf(0)
        private set
    var selectedSubtitle by mutableIntStateOf(0)
        private set
    var subtitleOffset by mutableLongStateOf(0L)
        private set
    var playWhenReady by mutableStateOf(true)

    val displayTitle: String
        get() = (mediaSession?.player?.mediaMetadata?.displayTitle ?: "").toString()

    private lateinit var currentFactory: DefaultMediaSourceFactory
    private var currentTextRenderer: CustomTextRenderer? = null
    private var simpleCache: SimpleCache? = null

    // == CCs/Audios/Qualities
    private val qualityTrackGroups: MutableList<Tracks.Group?> = mutableListOf()
    private val audioTrackGroups: MutableList<Tracks.Group?> = mutableListOf()
    val availableAudios = mutableStateListOf<String>()
    val availableSubtitles = mutableStateListOf<SubtitleConfiguration>()
    // ==

    init {
        disableSSLVerification()
    }

    override fun onEvents(player: Player, events: Player.Events) {
        duration = player.duration.coerceAtLeast(0L)
        currentPosition = player.currentPosition.coerceIn(0L, duration)
        playbackState = player.playbackState
        isPlaying = player.isPlaying
        bufferedPercentage = player.bufferedPercentage
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> onReady()
            Player.STATE_IDLE -> {
                // Re-prepare the player to
                // resolve internet connection issues
                mediaSession?.player?.run {
                    prepare()
                    playWhenReady = this@FlixclusivePlayer.playWhenReady
                }
            }

            else -> Unit
        }
    }

    @UnstableApi
    fun initialize() {
        debugLog("Initializing the player...")
        val trackSelector = DefaultTrackSelector(context)
        val loadControl =
            getLoadControl(
                bufferCacheSize = appSettings.preferredBufferCacheSize * 1024L * 1024L,
                videoBufferMs = appSettings.preferredVideoBufferMs * 1000L
            )

        currentFactory = getCacheFactory()

        val exoPlayer = ExoPlayer.Builder(context)
            .apply {
                setDeviceVolumeControlEnabled(true)
                setTrackSelector(trackSelector)

                setMediaSourceFactory(currentFactory)

                setRenderersFactory { eventHandler, videoRendererEventListener, audioRendererEventListener, textRendererOutput, metadataRendererOutput ->
                    context.getRenderers(
                        eventHandler = eventHandler,
                        videoRendererEventListener = videoRendererEventListener,
                        audioRendererEventListener = audioRendererEventListener,
                        textRendererOutput = textRendererOutput,
                        metadataRendererOutput = metadataRendererOutput,
                        subtitleOffset = subtitleOffset,
                        onTextRendererChange = {
                            currentTextRenderer = it
                        }
                    )
                }
                appSettings.preferredSeekAmount.let {
                    setSeekBackIncrementMs(it)
                    setSeekForwardIncrementMs(it)
                }

                setSeekParameters(SeekParameters(toleranceBeforeUs, toleranceAfterUs))
                setLoadControl(loadControl)
            }
            .build()
            .apply {
                setHandleAudioBecomingNoisy(true)
                addListener(this@FlixclusivePlayer)
            }

        mediaSession = MediaSession
            .Builder(context, exoPlayer)
            .build()
    }

    fun prepare(
        link: SourceLink,
        title: String,
        subtitles: List<Subtitle>,
        initialPlaybackPosition: Long = 0L,
    ) {
        (mediaSession!!.player as ExoPlayer).run {
            debugLog("Preparing the player...")

            extractSubtitles(subtitles)
            selectedSubtitle = when {
                !appSettings.isSubtitleEnabled -> 0
                else -> availableSubtitles
                    .getIndexFromLanguage(appSettings.subtitleLanguage)
            }

            val mediaSource = currentFactory.createMediaSource(
                getMediaItem(
                    url = link.url,
                    title = title
                )
            )

            setMediaSource(mediaSource, initialPlaybackPosition)
            prepare()
            playWhenReady = this@FlixclusivePlayer.playWhenReady
        }
    }

    fun release() {
        mediaSession?.run {
            debugLog("Releasing the player...")

            hasBeenInitialized = false
            clearTracks()

            player.removeListener(this@FlixclusivePlayer)
            simpleCache?.release()
            player.release()
            release()

            simpleCache = null
            mediaSession = null
            currentTextRenderer = null
        }
    }

    private fun onReady() {
        mediaSession?.player?.run {
            extractAudios()

            playbackParameters =
                PlaybackParameters(playbackSpeed)

            currentTextRenderer?.setRenderOffsetMs(offset = subtitleOffset)
            onSubtitleChange(index = selectedSubtitle)
            onAudioChange(index = selectedAudio)

            hasBeenInitialized = true
        }
    }

    private fun getCacheFactory(): DefaultMediaSourceFactory {
        val cacheFactory = CacheDataSource.Factory().apply {
            if(simpleCache == null) {
                val cacheSize = appSettings.preferredDiskCacheSize * 1024L * 1024L
                simpleCache = context.getCache(cacheSize)
            }

            simpleCache?.let(::setCache)

            val source = DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setAllowCrossProtocolRedirects(true)

            setUpstreamDataSourceFactory(source)
        }

        return DefaultMediaSourceFactory(cacheFactory)
    }

    private fun getMediaItem(
        url: String,
        title: String?
    ): MediaItem {
        return MediaItem.Builder()
            .apply {
                setUri(url)
                setMediaMetadata(
                    MediaMetadata.Builder()
                        .setDisplayTitle(title)
                        .build()
                )
                setSubtitleConfigurations(availableSubtitles)
            }
            .build()
    }

    private fun clearTracks() {
        audioTrackGroups.clear()
        qualityTrackGroups.clear()

        availableAudios.clear()
        availableSubtitles.clear()
    }

    /**
     * Extracts the provider audios by populating
     * the available audios and track groups.
     *
     */
    private fun extractAudios() {
        audioTrackGroups.clear()
        availableAudios.clear()

        mediaSession?.player?.run {
            audioTrackGroups.addAll(
                currentTracks.groups
                    .filter { group ->
                        group.type == C.TRACK_TYPE_AUDIO
                    }
            )

            audioTrackGroups.forEach { group ->
                group?.let {
                    for (trackIndex in 0 until it.length) {
                        val format = it.getTrackFormat(trackIndex)
                        val locale = Locale(format.language ?: "Default")

                        availableAudios.add(
                            "Audio Track #${availableAudios.size + 1}: ${locale.displayLanguage}"
                        )
                    }
                }
            }
        }
    }

    /**
     * extracts the subtitle configurations of it.
     *
     */
    private fun extractSubtitles(subtitles: List<Subtitle>) {
        availableSubtitles.clear()

        subtitles.addOffSubtitles().forEach { subtitle ->
            val subtitleConfiguration = SubtitleConfiguration
                .Builder(subtitle.url.toUri())
                .setMimeType(getSubtitleMimeType(subtitle))
                .setLanguage(subtitle.lang)
                .build()

            availableSubtitles.add(subtitleConfiguration)
        }
    }

    /**
     * Callback function triggered when the subtitles are changed.
     *
     * @param subtitleIndex The index of the selected subtitle.
     */
    fun onSubtitleChange(index: Int) {
        selectedSubtitle = index

        val oldTrackSelectionParameters = mediaSession?.player?.trackSelectionParameters

        oldTrackSelectionParameters?.let {
            mediaSession?.player?.trackSelectionParameters = when (selectedSubtitle) {
                // 0 == OFF Subtitles
                0 -> {
                    it.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                }
                else -> {
                    it.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setPreferredTextLanguage(availableSubtitles[selectedSubtitle].language)
                        .build()
                }
            }
        }
    }

    /**
     * Callback function triggered when the audio track is changed.
     *
     * @param audioIndex The index of the selected audio track.
     */
    fun onAudioChange(index: Int) {
        selectedAudio = index

        audioTrackGroups[selectedAudio]?.let { group ->
            mediaSession?.player?.run {
                trackSelectionParameters =
                    trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .setPreferredAudioLanguage(
                        group.getTrackFormat(0).language
                            ?: Locale.US.language
                    )
                    .build()
            }
        }
    }

    /**
     * Callback function triggered when the audio track is changed.
     * Though, this one needs the shorthand language name to change
     * the audio.
     *
     * @param language The language of the audio track
     */
    fun onAudioChange(language: String) {
        val index = availableAudios.indexOfFirst {
            it.contains(Locale(language).displayLanguage, ignoreCase = true)
        }

        onAudioChange(max(index, 0))
    }

    fun onPlaybackSpeedChange(speed: Int) {
        playbackSpeed = 1F + (speed * 0.25F)
        mediaSession?.player?.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    /**
     *
     * Adding delays on subtitle appearances
     * */
    fun onSubtitleOffsetChange(offset : Long) {
        subtitleOffset = offset
        currentTextRenderer?.setRenderOffsetMs(offset)
    }

    /**
     *
     * For handling broadcast events sent
     * by the Picture-in-Picture mode
     * */
    fun handleBroadcastEvents(event: Int) {
        mediaSession?.player?.run {
            when(event) {
                PlayerEvents.PLAY.ordinal -> {
                    play()
                    this@FlixclusivePlayer.playWhenReady = true
                }
                PlayerEvents.PAUSE.ordinal -> {
                    pause()
                    this@FlixclusivePlayer.playWhenReady = false
                }
                PlayerEvents.REPLAY.ordinal -> {
                    // Replay the video
                    seekTo(0)
                    this@FlixclusivePlayer.playWhenReady = true
                }
                PlayerEvents.FORWARD.ordinal -> seekForward()
                PlayerEvents.BACKWARD.ordinal -> seekBack()
            }
        }
    }

    fun play(){
        mediaSession?.player?.play()
    }

    fun pause(){
        mediaSession?.player?.pause()
    }

    fun addSubtitle(subtitle: Subtitle) {
        val mimeType = getSubtitleMimeType(subtitle)

        availableSubtitles.add(
            index = 1,
            element = SubtitleConfiguration
                .Builder(subtitle.url.toUri())
                .setMimeType(mimeType)
                .setLanguage(subtitle.lang.lowercase())
                //.setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        )
    }

    fun setDeviceVolume(volume: Int) {
        mediaSession?.player?.setDeviceVolume(volume, 0)
    }

    fun seekTo(position: Long) {
        mediaSession?.player?.seekTo(position)
    }

    fun seekBack() {
        mediaSession?.player?.seekBack()
    }

    fun seekForward() {
        mediaSession?.player?.seekForward()
    }

    fun getPlayer(): Player? = mediaSession?.player

    /**
     *
     * Gets the current time of the player
     * in terms of where the position of the
     * player is.
     *
     * This will be used for the time bar/slider
     *
     */
    suspend fun observePlayerPosition() {
        mediaSession?.player?.run {
            while(isPlaying) {
                this@FlixclusivePlayer.currentPosition = currentPosition
                delay(1.seconds / 30) // Important to add delay
            }
        }
    }


    /**
     *
     * Initializes the subtitle view's
     * subtitle style based on user's
     * preferences
     *
     * */
    fun setSubtitleStyle(
        subtitleView: SubtitleView?,
        areControlsVisible: Boolean,
        isInPictureInPictureMode: Boolean = false,
        isInTv: Boolean = false,
    ) {
        subtitleView?.run {
            // Add margin on subtitle view
            var subtitleMarginBottom = 0.05F

            if (areControlsVisible && !isInTv) {
                subtitleMarginBottom += 0.15F
            } else if (areControlsVisible) {
                subtitleMarginBottom += 0.22F
            }

            setBottomPaddingFraction(subtitleMarginBottom)

            // Modify subtitle style
            val style = CaptionStyleCompat(
                appSettings.subtitleColor,
                appSettings.subtitleBackgroundColor,
                Color.TRANSPARENT,
                appSettings.subtitleEdgeType.type,
                Color.BLACK,
                appSettings.subtitleFontStyle.typeface
            )

            val fontSize = if (isInPictureInPictureMode)
                FONT_SIZE_PIP_MODE
            else appSettings.subtitleSize.getDp(isInTv)

            setApplyEmbeddedFontSizes(false)
            setApplyEmbeddedStyles(false)
            setStyle(style)
            //translationY = 20.toPx.toFloat()
            setFixedTextSize(
                /* unit = */ TypedValue.COMPLEX_UNIT_SP,
                /* size = */ fontSize
            )
        }
    }
}