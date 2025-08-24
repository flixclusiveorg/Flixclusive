package com.flixclusive.core.presentation.player

import android.content.Context
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.SubtitleView
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.player.PlayerQuality.Companion.getIndexOfPreferredQuality
import com.flixclusive.core.presentation.player.extensions.isLiveError
import com.flixclusive.core.presentation.player.extensions.isNetworkException
import com.flixclusive.core.presentation.player.extensions.setStyle
import com.flixclusive.core.presentation.player.model.PlaylistItem
import com.flixclusive.core.presentation.player.model.PlaylistItem.Companion.toMediaItem
import com.flixclusive.core.presentation.player.util.internal.PlayerBuilderHelper.disableSSLVerification
import com.flixclusive.core.presentation.player.util.internal.PlayerBuilderHelper.getCacheFactory
import com.flixclusive.core.presentation.player.util.internal.PlayerBuilderHelper.getLoadControl
import com.flixclusive.core.presentation.player.util.internal.PlayerBuilderHelper.getRenderers
import com.flixclusive.core.presentation.player.util.internal.PlayerCacheManager
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.network.okhttp.USER_AGENT
import com.flixclusive.core.util.network.okhttp.UserAgentManager
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode
import okhttp3.OkHttpClient
import javax.inject.Inject

private const val TOLERANCE_BEFORE_US = 300_000L
private const val TOLERANCE_AFTER_US = 300_000L

/**
 * A wrapper class for [Player] functionalities.
 *
 * This class is intended to be used for Jetpack Compose UIs.
 */
@Suppress("DEPRECATION")
@OptIn(UnstableApi::class)
internal class InternalPlayerImpl
    @Inject
    constructor(
        client: OkHttpClient,
        private val context: Context,
        private val subtitlePrefs: SubtitlesPreferences,
        private val playerPrefs: PlayerPreferences,
    ) : SubtitleOffsetProvider,
        InternalPlayer {
        override var areTracksInitialized: Boolean = false
            internal set

        override var isInitialized: Boolean = false
            private set

        override var currentSubtitleOffset by mutableLongStateOf(0L)

        override var subtitleView: SubtitleView? = null

        private val playlist = mutableSetOf<PlaylistItem>()

        /** Only internally visible so ComposePlayer component can access it */
        var exoPlayer: ExoPlayer? by mutableStateOf(null)

        private var mediaSession: MediaSession? = null
        private val listener = InternalPlayerListener()

        private var textRenderer: TextRenderer? = null

        /** The currently selected stream index from the playlist item. */
        private var selectedStreamIndex: Int = 0

        /** Backing property for playWhenReady to keep the value when the player is null. */
        private var _playWhenReady: Boolean = true

        private lateinit var cacheFactory: DefaultMediaSourceFactory
        private val playerCacheManager = PlayerCacheManager(context)
        private val localDataSource: DefaultDataSource.Factory
        private val okHttpDataSource: OkHttpDataSource.Factory
        private val baseHttpDataSource = DefaultHttpDataSource
            .Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)

        init {
            localDataSource = DefaultDataSource.Factory(context, baseHttpDataSource)
            okHttpDataSource = OkHttpDataSource.Factory(client).setUserAgent(UserAgentManager.getRandomUserAgent())
        }

        override fun initialize() {
            if (exoPlayer != null) return

            infoLog("Initializing the player...")
            disableSSLVerification()

            val trackSelector = DefaultTrackSelector(context)
            val loadControl = getLoadControl(
                bufferCacheSize = playerPrefs.bufferCacheSize,
                videoBufferMs = playerPrefs.videoBufferMs,
            )

            cacheFactory = context.getCacheFactory(
                cache = playerCacheManager.getCache(
                    preferredDiskCacheSize = playerPrefs.diskCacheSize,
                ),
                onlineDataSource = okHttpDataSource,
            )

            exoPlayer = ExoPlayer
                .Builder(context)
                .apply {
                    setDeviceVolumeControlEnabled(true)
                    setTrackSelector(trackSelector)
                    setMediaSourceFactory(cacheFactory)
                    setRenderersFactory {
                        eventHandler,
                        videoRendererEventListener,
                        audioRendererEventListener,
                        textRendererOutput,
                        metadataRendererOutput,
                        ->
                        context.getRenderers(
                            eventHandler = eventHandler,
                            videoRendererEventListener = videoRendererEventListener,
                            audioRendererEventListener = audioRendererEventListener,
                            textRendererOutput = textRendererOutput,
                            metadataRendererOutput = metadataRendererOutput,
                            subtitleOffsetProvider = this@InternalPlayerImpl,
                            decoderPriority = playerPrefs.decoderPriority,
                            onTextRendererChange = { textRenderer = it },
                        )
                    }
                    playerPrefs.seekAmount.let {
                        setSeekBackIncrementMs(it)
                        setSeekForwardIncrementMs(it)
                    }
                    setSeekParameters(SeekParameters(TOLERANCE_BEFORE_US, TOLERANCE_AFTER_US))
                    setLoadControl(loadControl)
                }.build()
                .apply {
                    setHandleAudioBecomingNoisy(true)
                    addListener(listener)
                }

            infoLog("Initializing the media session...")
            mediaSession = MediaSession.Builder(context, exoPlayer!!).build()

            subtitleView?.let {
                infoLog("Initializing SubtitleView...")
                it.setStyle(subtitlePrefs)
                it.setCues(null)

                if (isCommandAvailable(Player.COMMAND_GET_TEXT)) {
                    it.setCues(currentCues.cues)
                }
            }

            isInitialized = true
            infoLog("Player initialized successfully!")
        }

        override fun prepare(
            item: PlaylistItem,
            position: Long,
        ) {
            if (exoPlayer == null) return

            infoLog("Preparing the player...")

            if (item.id != playlist.lastOrNull()?.id) {
                if (item in playlist) {
                    playlist.remove(item)
                }

                playlist.add(item)
                selectedStreamIndex = item.streams.getIndexOfPreferredQuality(preference = playerPrefs.quality)
                areTracksInitialized = false
            } else {
                infoLog("Re-preparing the same item, keeping the same stream...")
            }

            val stream = item.streams[selectedStreamIndex]
            val customHeaders = stream.customHeaders ?: emptyMap()
            okHttpDataSource.setDefaultRequestProperties(customHeaders)
            baseHttpDataSource.setDefaultRequestProperties(customHeaders)

            val mediaItem = item.toMediaItem(selectedStreamIndex)
            val mediaSource = cacheFactory.createMediaSource(mediaItem)

            exoPlayer?.setMediaSource(mediaSource, position)
            prepare()
            playWhenReady = _playWhenReady
        }

        override fun release() {
            if (playerPrefs.isForcingPlayerRelease) {
                infoLog("Releasing the player...")
                isInitialized = false
                playerCacheManager.releaseCache()
                removeListener(listener)
                exoPlayer?.release()
                exoPlayer = null
                textRenderer = null
            }

            infoLog("Releasing the media session...")
            mediaSession?.release()
            mediaSession = null
        }

        override fun getPlaylistItem(
            metadata: FilmMetadata,
            episode: Episode?,
        ): PlaylistItem? {
            val id = PlaylistItem.createId(metadata, episode)

            return playlist.firstOrNull { it.id == id }
        }

        override fun selectStream(index: Int) {
            if (exoPlayer == null) return

            val currentItem = playlist.lastOrNull() ?: return
            if (index < 0 || index >= currentItem.streams.size) return

            selectedStreamIndex = index
            prepare(currentItem, exoPlayer?.currentPosition ?: 0L)
        }

        /**
         * Changes the delay offset of subtitles for syncing purposes
         *
         * @param offset The delay offset to apply on the subtitle cues
         * */
        override fun changeSubtitleDelay(offset: Long) {
            currentSubtitleOffset = offset

            // Apply the offset change immediately to the current text renderer
            if (textRenderer?.state == TextRenderer.STATE_ENABLED ||
                textRenderer?.state == TextRenderer.STATE_STARTED
            ) {
                // Force the text renderer to re-render with the new offset
                // by resetting its position to the current playback position
                val currentPos = exoPlayer?.currentPosition ?: 0L

                // The renderer will pick up the new offset from the updated state
                // when it re-renders the current position
                try {
                    textRenderer?.resetPosition(currentPos)
                } catch (e: Exception) {
                    // Fallback: seek slightly to trigger subtitle re-rendering
                    exoPlayer?.let { currentPlayer ->
                        val seekPos = (currentPos - 50).coerceAtLeast(0L)
                        currentPlayer.seekTo(seekPos)
                        currentPlayer.seekTo(currentPos)
                    }
                }
            }
        }

        override fun handlePiPEvent(event: Int) {
            when (event) {
                PiPEvent.PLAY.ordinal -> {
                    play()
                    _playWhenReady = true
                }

                PiPEvent.PAUSE.ordinal -> {
                    pause()
                    _playWhenReady = false
                }

                PiPEvent.REPLAY.ordinal -> {
                    seekTo(0)
                    _playWhenReady = true
                }

                PiPEvent.FORWARD.ordinal -> seekForward()
                PiPEvent.BACKWARD.ordinal -> seekBack()
            }
        }

        private inner class InternalPlayerListener : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> changeSubtitleDelay(0)
                    else -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val isDurationNotUnset = exoPlayer?.duration != null && exoPlayer?.duration != C.TIME_UNSET
                if (error.isNetworkException() && isDurationNotUnset || error.isLiveError()) {
                    exoPlayer?.seekToDefaultPosition()
                    exoPlayer?.prepare()
                    exoPlayer?.playWhenReady = _playWhenReady
                }
            }

            override fun onCues(cueGroup: CueGroup) {
                subtitleView?.setStyle(subtitlePrefs)
                subtitleView?.setCues(cueGroup.cues)
            }
        }

        override fun setPlayWhenReady(value: Boolean) {
            _playWhenReady = value
            exoPlayer?.playWhenReady = value
        }

        // TODO: Don't scroll down anymore pls

        override fun play() {
            exoPlayer?.play()
        }

        override fun pause() {
            exoPlayer?.pause()
        }

        override fun seekTo(position: Long) {
            exoPlayer?.seekTo(position)
        }

        override fun seekBack() {
            exoPlayer?.seekBack()
        }

        override fun seekForward() {
            exoPlayer?.seekForward()
        }

        override fun prepare() {
            exoPlayer?.prepare()
        }

        override fun seekTo(
            mediaItemIndex: Int,
            positionMs: Long,
        ) {
            exoPlayer?.seekTo(mediaItemIndex, positionMs)
        }

        override fun getApplicationLooper(): Looper {
            return exoPlayer?.applicationLooper ?: Looper.getMainLooper()
        }

        override fun addListener(listener: Player.Listener) {
            exoPlayer?.addListener(listener)
        }

        override fun removeListener(listener: Player.Listener) {
            exoPlayer?.removeListener(listener)
        }

        override fun setMediaItems(mediaItems: List<MediaItem>) {
            exoPlayer?.setMediaItems(mediaItems)
        }

        override fun setMediaItems(
            mediaItems: List<MediaItem>,
            resetPosition: Boolean,
        ) {
            exoPlayer?.setMediaItems(mediaItems, resetPosition)
        }

        override fun setMediaItems(
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ) {
            exoPlayer?.setMediaItems(mediaItems, startIndex, startPositionMs)
        }

        override fun setMediaItem(mediaItem: MediaItem) {
            exoPlayer?.setMediaItem(mediaItem)
        }

        override fun setMediaItem(
            mediaItem: MediaItem,
            startPositionMs: Long,
        ) {
            exoPlayer?.setMediaItem(mediaItem, startPositionMs)
        }

        override fun setMediaItem(
            mediaItem: MediaItem,
            resetPosition: Boolean,
        ) {
            exoPlayer?.setMediaItem(mediaItem, resetPosition)
        }

        override fun addMediaItem(mediaItem: MediaItem) {
            exoPlayer?.addMediaItem(mediaItem)
        }

        override fun addMediaItem(
            index: Int,
            mediaItem: MediaItem,
        ) {
            exoPlayer?.addMediaItem(index, mediaItem)
        }

        override fun addMediaItems(mediaItems: List<MediaItem>) {
            exoPlayer?.addMediaItems(mediaItems)
        }

        override fun addMediaItems(
            index: Int,
            mediaItems: List<MediaItem>,
        ) {
            exoPlayer?.addMediaItems(index, mediaItems)
        }

        override fun moveMediaItem(
            currentIndex: Int,
            newIndex: Int,
        ) {
            exoPlayer?.moveMediaItem(currentIndex, newIndex)
        }

        override fun moveMediaItems(
            fromIndex: Int,
            toIndex: Int,
            newIndex: Int,
        ) {
            exoPlayer?.moveMediaItems(fromIndex, toIndex, newIndex)
        }

        override fun replaceMediaItem(
            index: Int,
            mediaItem: MediaItem,
        ) {
            exoPlayer?.replaceMediaItem(index, mediaItem)
        }

        override fun replaceMediaItems(
            fromIndex: Int,
            toIndex: Int,
            mediaItems: List<MediaItem>,
        ) {
            exoPlayer?.replaceMediaItems(fromIndex, toIndex, mediaItems)
        }

        override fun removeMediaItem(index: Int) {
            exoPlayer?.removeMediaItem(index)
        }

        override fun removeMediaItems(
            fromIndex: Int,
            toIndex: Int,
        ) {
            exoPlayer?.removeMediaItems(fromIndex, toIndex)
        }

        override fun clearMediaItems() {
            exoPlayer?.clearMediaItems()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            return exoPlayer?.isCommandAvailable(command) ?: false
        }

        override fun canAdvertiseSession(): Boolean {
            return exoPlayer?.canAdvertiseSession() ?: false
        }

        override fun getAvailableCommands(): Player.Commands {
            return exoPlayer?.availableCommands ?: Player.Commands.EMPTY
        }

        override fun getPlaybackState(): Int {
            return exoPlayer?.playbackState ?: Player.STATE_IDLE
        }

        override fun getPlaybackSuppressionReason(): Int {
            return exoPlayer?.playbackSuppressionReason ?: Player.PLAYBACK_SUPPRESSION_REASON_NONE
        }

        override fun isPlaying(): Boolean {
            return exoPlayer?.isPlaying ?: false
        }

        override fun getPlayerError(): PlaybackException? {
            return exoPlayer?.playerError
        }

        override fun getPlayWhenReady(): Boolean {
            return exoPlayer?.playWhenReady ?: false
        }

        override fun setRepeatMode(repeatMode: Int) {
            exoPlayer?.repeatMode = repeatMode
        }

        override fun getRepeatMode(): Int {
            return exoPlayer?.repeatMode ?: Player.REPEAT_MODE_OFF
        }

        override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
            exoPlayer?.shuffleModeEnabled = shuffleModeEnabled
        }

        override fun getShuffleModeEnabled(): Boolean {
            return exoPlayer?.shuffleModeEnabled ?: false
        }

        override fun isLoading(): Boolean {
            return exoPlayer?.isLoading ?: false
        }

        override fun seekToDefaultPosition() {
            exoPlayer?.seekToDefaultPosition()
        }

        override fun seekToDefaultPosition(mediaItemIndex: Int) {
            exoPlayer?.seekToDefaultPosition(mediaItemIndex)
        }

        override fun getSeekBackIncrement(): Long {
            return exoPlayer?.seekBackIncrement ?: 0L
        }

        override fun getSeekForwardIncrement(): Long {
            return exoPlayer?.seekForwardIncrement ?: 0L
        }

        override fun hasPreviousMediaItem(): Boolean {
            return exoPlayer?.hasPreviousMediaItem() ?: false
        }

        override fun seekToPreviousMediaItem() {
            exoPlayer?.seekToPreviousMediaItem()
        }

        override fun getMaxSeekToPreviousPosition(): Long {
            return exoPlayer?.maxSeekToPreviousPosition ?: 0L
        }

        override fun seekToPrevious() {
            exoPlayer?.seekToPrevious()
        }

        override fun hasNextMediaItem(): Boolean {
            return exoPlayer?.hasNextMediaItem() ?: false
        }

        override fun seekToNextMediaItem() {
            exoPlayer?.seekToNextMediaItem()
        }

        override fun seekToNext() {
            exoPlayer?.seekToNext()
        }

        override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
            exoPlayer?.playbackParameters = playbackParameters
        }

        override fun setPlaybackSpeed(speed: Float) {
            exoPlayer?.setPlaybackSpeed(speed)
        }

        override fun getPlaybackParameters(): PlaybackParameters {
            return exoPlayer?.playbackParameters ?: PlaybackParameters.DEFAULT
        }

        override fun stop() {
            exoPlayer?.stop()
        }

        override fun getCurrentTracks(): Tracks {
            return exoPlayer?.currentTracks ?: Tracks.EMPTY
        }

        override fun getTrackSelectionParameters(): TrackSelectionParameters {
            return exoPlayer?.trackSelectionParameters ?: TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
        }

        override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
            exoPlayer?.trackSelectionParameters = parameters
        }

        override fun getMediaMetadata(): MediaMetadata {
            return exoPlayer?.mediaMetadata ?: MediaMetadata.EMPTY
        }

        override fun getPlaylistMetadata(): MediaMetadata {
            return exoPlayer?.playlistMetadata ?: MediaMetadata.EMPTY
        }

        override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
            exoPlayer?.playlistMetadata = mediaMetadata
        }

        override fun getCurrentManifest(): Any? {
            return exoPlayer?.currentManifest
        }

        override fun getCurrentTimeline(): Timeline {
            return exoPlayer?.currentTimeline ?: Timeline.EMPTY
        }

        override fun getCurrentPeriodIndex(): Int {
            return exoPlayer?.currentPeriodIndex ?: 0
        }

        @Deprecated("Deprecated in Java")
        override fun getCurrentWindowIndex(): Int {
            return exoPlayer?.currentWindowIndex ?: 0
        }

        override fun getCurrentMediaItemIndex(): Int {
            return exoPlayer?.currentMediaItemIndex ?: 0
        }

        @Deprecated("Deprecated in Java")
        override fun getNextWindowIndex(): Int {
            return exoPlayer?.nextWindowIndex ?: C.INDEX_UNSET
        }

        override fun getNextMediaItemIndex(): Int {
            return exoPlayer?.nextMediaItemIndex ?: C.INDEX_UNSET
        }

        @Deprecated("Deprecated in Java")
        override fun getPreviousWindowIndex(): Int {
            return exoPlayer?.previousWindowIndex ?: C.INDEX_UNSET
        }

        override fun getPreviousMediaItemIndex(): Int {
            return exoPlayer?.previousMediaItemIndex ?: C.INDEX_UNSET
        }

        override fun getCurrentMediaItem(): MediaItem? {
            return exoPlayer?.currentMediaItem
        }

        override fun getMediaItemCount(): Int {
            return exoPlayer?.mediaItemCount ?: 0
        }

        override fun getMediaItemAt(index: Int): MediaItem {
            return exoPlayer?.getMediaItemAt(index) ?: MediaItem.EMPTY
        }

        override fun getDuration(): Long {
            return exoPlayer?.duration ?: C.TIME_UNSET
        }

        override fun getCurrentPosition(): Long {
            return exoPlayer?.currentPosition ?: 0L
        }

        override fun getBufferedPosition(): Long {
            return exoPlayer?.bufferedPosition ?: 0L
        }

        override fun getBufferedPercentage(): Int {
            return exoPlayer?.bufferedPercentage ?: 0
        }

        override fun getTotalBufferedDuration(): Long {
            return exoPlayer?.totalBufferedDuration ?: 0L
        }

        @Deprecated("Deprecated in Java")
        override fun isCurrentWindowDynamic(): Boolean {
            return exoPlayer?.isCurrentWindowDynamic ?: false
        }

        override fun isCurrentMediaItemDynamic(): Boolean {
            return exoPlayer?.isCurrentMediaItemDynamic ?: false
        }

        @Deprecated("Deprecated in Java")
        override fun isCurrentWindowLive(): Boolean {
            return exoPlayer?.isCurrentWindowLive ?: false
        }

        override fun isCurrentMediaItemLive(): Boolean {
            return exoPlayer?.isCurrentMediaItemLive ?: false
        }

        override fun getCurrentLiveOffset(): Long {
            return exoPlayer?.currentLiveOffset ?: C.TIME_UNSET
        }

        @Deprecated("Deprecated in Java")
        override fun isCurrentWindowSeekable(): Boolean {
            return exoPlayer?.isCurrentWindowSeekable ?: false
        }

        override fun isCurrentMediaItemSeekable(): Boolean {
            return exoPlayer?.isCurrentMediaItemSeekable ?: false
        }

        override fun isPlayingAd(): Boolean {
            return exoPlayer?.isPlayingAd ?: false
        }

        override fun getCurrentAdGroupIndex(): Int {
            return exoPlayer?.currentAdGroupIndex ?: C.INDEX_UNSET
        }

        override fun getCurrentAdIndexInAdGroup(): Int {
            return exoPlayer?.currentAdIndexInAdGroup ?: C.INDEX_UNSET
        }

        override fun getContentDuration(): Long {
            return exoPlayer?.contentDuration ?: C.TIME_UNSET
        }

        override fun getContentPosition(): Long {
            return exoPlayer?.contentPosition ?: 0L
        }

        override fun getContentBufferedPosition(): Long {
            return exoPlayer?.contentBufferedPosition ?: 0L
        }

        override fun getAudioAttributes(): AudioAttributes {
            return exoPlayer?.audioAttributes ?: AudioAttributes.DEFAULT
        }

        override fun setVolume(volume: Float) {
            exoPlayer?.volume = volume
        }

        override fun getVolume(): Float {
            return exoPlayer?.volume ?: 1f
        }

        override fun clearVideoSurface() {
            exoPlayer?.clearVideoSurface()
        }

        override fun clearVideoSurface(surface: Surface?) {
            exoPlayer?.clearVideoSurface(surface)
        }

        override fun setVideoSurface(surface: Surface?) {
            exoPlayer?.setVideoSurface(surface)
        }

        override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
            exoPlayer?.setVideoSurfaceHolder(surfaceHolder)
        }

        override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
            exoPlayer?.clearVideoSurfaceHolder(surfaceHolder)
        }

        override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
            exoPlayer?.setVideoSurfaceView(surfaceView)
        }

        override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
            exoPlayer?.clearVideoSurfaceView(surfaceView)
        }

        override fun setVideoTextureView(textureView: TextureView?) {
            exoPlayer?.setVideoTextureView(textureView)
        }

        override fun clearVideoTextureView(textureView: TextureView?) {
            exoPlayer?.clearVideoTextureView(textureView)
        }

        override fun getVideoSize(): VideoSize {
            return exoPlayer?.videoSize ?: VideoSize.UNKNOWN
        }

        override fun getSurfaceSize(): Size {
            return exoPlayer?.surfaceSize ?: Size.UNKNOWN
        }

        override fun getCurrentCues(): CueGroup {
            return exoPlayer?.currentCues ?: CueGroup.EMPTY_TIME_ZERO
        }

        override fun getDeviceInfo(): DeviceInfo {
            return exoPlayer?.deviceInfo ?: DeviceInfo.UNKNOWN
        }

        override fun getDeviceVolume(): Int {
            return exoPlayer?.deviceVolume ?: 0
        }

        override fun isDeviceMuted(): Boolean {
            return exoPlayer?.isDeviceMuted ?: false
        }

        @Deprecated("Deprecated in Java")
        override fun setDeviceVolume(volume: Int) {
            exoPlayer?.deviceVolume = volume
        }

        override fun setDeviceVolume(
            volume: Int,
            flags: Int,
        ) {
            exoPlayer?.setDeviceVolume(volume, flags)
        }

        @Deprecated("Deprecated in Java")
        override fun increaseDeviceVolume() {
            exoPlayer?.increaseDeviceVolume()
        }

        override fun increaseDeviceVolume(flags: Int) {
            exoPlayer?.increaseDeviceVolume(flags)
        }

        @Deprecated("Deprecated in Java")
        override fun decreaseDeviceVolume() {
            exoPlayer?.decreaseDeviceVolume()
        }

        override fun decreaseDeviceVolume(flags: Int) {
            exoPlayer?.decreaseDeviceVolume(flags)
        }

        @Deprecated("Deprecated in Java")
        override fun setDeviceMuted(muted: Boolean) {
            exoPlayer?.isDeviceMuted = muted
        }

        override fun setDeviceMuted(
            muted: Boolean,
            flags: Int,
        ) {
            exoPlayer?.setDeviceMuted(muted, flags)
        }

        override fun setAudioAttributes(
            audioAttributes: AudioAttributes,
            handleAudioFocus: Boolean,
        ) {
            exoPlayer?.setAudioAttributes(audioAttributes, handleAudioFocus)
        }
    }
