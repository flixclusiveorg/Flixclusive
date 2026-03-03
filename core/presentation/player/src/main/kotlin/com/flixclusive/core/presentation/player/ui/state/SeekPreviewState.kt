package com.flixclusive.core.presentation.player.ui.state

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import androidx.media3.inspector.FrameExtractor
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.util.log.errorLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    private val frameCache = LinkedHashMap<Long, Bitmap>(MAX_CACHE_SIZE, 0.75f, true)
    private val mutex = Mutex()
    private var extractionJob: Job? = null
    private var lastRequestedPositionMs by mutableLongStateOf(-1L)

    var currentFrame by mutableStateOf<Bitmap?>(null)
        private set

    fun onScrubbing(positionMs: Long, scope: CoroutineScope) {
        val snappedPosition = (positionMs / FRAME_INTERVAL_MS) * FRAME_INTERVAL_MS
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
                val mediaItem = withContext(main) { player.currentMediaItem } ?: return@runCatching
                val extractor = frameExtractor ?: FrameExtractor
                    .Builder(context, mediaItem)
                    .build()

                extractor.use {
                    val frame = it.getFrame(snappedPosition).get()
                    val bitmap = frame.bitmap

                    currentFrame = bitmap
                    mutex.withLock {
                        if (frameCache.size >= MAX_CACHE_SIZE) {
                            val oldest = frameCache.keys.first()
                            frameCache.remove(oldest)?.recycle()
                        }

                        frameCache[snappedPosition] = bitmap
                    }
                }
            }.onFailure {
                errorLog(it)
            }
        }
    }

    fun onScrubEnd() {
        extractionJob?.cancel()
        extractionJob = null

        frameExtractor?.close()
        frameExtractor = null
    }

    companion object {
        private val io = Dispatchers.IO
        private val main = Dispatchers.Main
        const val FRAME_INTERVAL_MS = 500L

        @Composable
        fun rememberSeekPreviewState(player: AppPlayer): SeekPreviewState {
            val context = LocalContext.current
            val state = remember(player) { SeekPreviewState(context, player) }

            return state
        }
    }
}
