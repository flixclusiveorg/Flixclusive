package com.flixclusive.core.presentation.player.ui.state

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.inspector.FrameExtractor
import com.flixclusive.core.util.log.errorLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val MAX_CACHE_SIZE = 30

@OptIn(UnstableApi::class)
@Stable
class SeekPreviewState(
    private val context: Context,
    private val player: AppPlayer,
) {
    private var frameExtractor: FrameExtractor? = null
        get() {
            if (field == null || field?.released?.get() == true) {
                val mediaItem = player.currentMediaItem ?: return null
                field = FrameExtractor.Builder(context, mediaItem)
                    .setDataSourceFactory(player.dataSourceFactory.remote)
                    .build()
            }

            return field
        }

    private val frameCache = LinkedHashMap<Long, Bitmap>(MAX_CACHE_SIZE, 0.75f, true)
    private val mutex = Mutex()
    private var extractionJob: Job? = null
    private var lastRequestedPositionMs by mutableLongStateOf(-1L)

    /**
     * The frame interval in milliseconds for which the preview frames are extracted.
     *
     * - If duration >= 4 hours, use 10 seconds
     * - If duration >= 2 hours, use 5 seconds
     * - If duration >= 1 hour, use 3 seconds
     * - If duration >= 30 minutes, use 2 seconds
     * - Otherwise, use 1 second
     * */
    val frameIntervalMs: Long
        get() {
            return when {
                player.duration == C.TIME_UNSET -> 1000L
                player.duration >= 4 * 60 * 60 * 1000L -> 10000L
                player.duration >= 2 * 60 * 60 * 1000L -> 5000L
                player.duration >= 1 * 60 * 60 * 1000L -> 3000L
                player.duration >= 30 * 60 * 1000L -> 2000L
                else -> 1000L
            }
        }

    var currentFrame by mutableStateOf<Bitmap?>(null)
        private set

    private suspend fun observe() {
        player.listen { events ->
            if (!events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) return@listen

            onScrubEnd()
            frameExtractor?.close()
            frameExtractor = null
        }
    }

    fun onScrubbing(positionMs: Long, scope: CoroutineScope) {
        val snappedPosition = getSnappedPosition(positionMs, frameIntervalMs)
        if (snappedPosition == lastRequestedPositionMs) return
        lastRequestedPositionMs = snappedPosition

        val cached = frameCache[snappedPosition]
        if (cached != null) {
            currentFrame = cached
            return
        }

        extractionJob?.cancel()
        extractionJob = scope.launch(io) {
            runCatching {
                val extractor = withContext(main) { frameExtractor } ?: return@runCatching
                val frame = extractor.getFrame(snappedPosition).get()
                val bitmap = frame?.bitmap ?: return@runCatching

                currentFrame = bitmap
                mutex.withLock {
                    if (frameCache.size >= MAX_CACHE_SIZE) {
                        val oldest = frameCache.keys.first()
                        frameCache.remove(oldest)?.recycle()
                    }

                    frameCache[snappedPosition] = bitmap
                }
            }.onFailure {
                errorLog(it)
            }
        }
    }

    fun onScrubEnd() {
        extractionJob?.cancel()
        extractionJob = null
    }

    private fun release() {
        onScrubEnd()
        frameExtractor?.close()
        frameExtractor = null
        frameCache.values.forEach { it.recycle() }
        frameCache.clear()
    }

    companion object {
        private val io = Dispatchers.IO
        private val main = Dispatchers.Main

        fun getSnappedPosition(positionMs: Long, frameIntervalMs: Long): Long {
            return (positionMs / frameIntervalMs) * frameIntervalMs
        }

        @Composable
        fun rememberSeekPreviewState(
            player: AppPlayer,
            key: () -> String
        ): SeekPreviewState {
            val context = LocalContext.current
            val state = remember(player) { SeekPreviewState(context, player) }

            LaunchedEffect(player) { state.observe() }

            LaunchedEffect(player) {
                var previousKey: String? = null

                snapshotFlow(key)
                    .distinctUntilChanged()
                    .collect {
                        if (it != previousKey) {
                            state.release()
                            previousKey = it
                        }
                    }
            }

            DisposableEffect(player) {
                onDispose {
                    state.release()
                }
            }

            return state
        }
    }
}
