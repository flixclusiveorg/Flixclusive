package com.flixclusive.data.downloads.transfer.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.downloads.DownloadChunk
import com.flixclusive.core.database.entity.downloads.DownloadChunkStatus
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.downloads.transfer.MediaTransferEngine
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.flixclusive.data.downloads.transfer.RangeUnsupportedException
import com.hippo.unifile.UniFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import com.flixclusive.data.downloads.di.DownloadHttpClient
import com.flixclusive.data.downloads.util.okRequest
import okhttp3.OkHttpClient
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import javax.inject.Inject

internal class MediaTransferEngineImpl @Inject constructor(
    @param:DownloadHttpClient private val client: OkHttpClient,
    @param:ApplicationContext private val context: Context,
    private val appDispatchers: AppDispatchers,
) : MediaTransferEngine {
    override suspend fun transfer(
        chunks: List<DownloadChunk>,
        url: String,
        headers: Map<String, String>,
        destinationFile: UniFile,
        shouldInterrupt: () -> Boolean,
        onChunkProgress: suspend (Long, Long, DownloadChunkStatus) -> Unit,
    ): MediaTransferResult =
        withContext(appDispatchers.io) {
            val outcomes =
                coroutineScope {
                    chunks
                        .filter { it.status != DownloadChunkStatus.COMPLETED }
                        .map { chunk ->
                            async {
                                transferChunk(chunk, url, headers, destinationFile, shouldInterrupt, onChunkProgress)
                            }
                        }.awaitAll()
                }

            when {
                outcomes.any { it is ChunkOutcome.Interrupted } -> MediaTransferResult.Cancelled
                outcomes.all { it is ChunkOutcome.Completed } -> MediaTransferResult.Completed
                // The real cause, not a synthetic summary: callers classify on it to decide whether
                // the link is at fault or the environment is (no space, no network, no permission).
                else -> MediaTransferResult.Failed(
                    outcomes.filterIsInstance<ChunkOutcome.Failed>().first().cause,
                )
            }
        }

    private suspend fun transferChunk(
        chunk: DownloadChunk,
        url: String,
        headers: Map<String, String>,
        destinationFile: UniFile,
        shouldInterrupt: () -> Boolean,
        onChunkProgress: suspend (Long, Long, DownloadChunkStatus) -> Unit,
    ): ChunkOutcome {
        var offset = chunk.bytesDownloaded
        var attempt = 0
        var lastError: Throwable = IOException("Chunk ${chunk.chunkIndex} failed to download")

        while (attempt < MAX_CHUNK_RETRIES) {
            try {
                val completed =
                    downloadChunkOnce(chunk, offset, url, headers, destinationFile, shouldInterrupt) { bytesWritten ->
                        offset = bytesWritten
                        onChunkProgress(chunk.id, bytesWritten, DownloadChunkStatus.DOWNLOADING)
                    }

                if (completed) {
                    onChunkProgress(chunk.id, offset, DownloadChunkStatus.COMPLETED)
                    return ChunkOutcome.Completed
                }

                return ChunkOutcome.Interrupted
            } catch (e: CancellationException) {
                // Never swallow cancellation into the retry loop — that would keep re-issuing
                // requests for a transfer whose scope has already been torn down.
                throw e
            } catch (e: RangeUnsupportedException) {
                // Retrying cannot help: the server will answer the same way every time.
                errorLog("Chunk ${chunk.chunkIndex} aborted: ${e.message}")
                onChunkProgress(chunk.id, offset, DownloadChunkStatus.FAILED)
                return ChunkOutcome.Failed(e)
            } catch (e: Throwable) {
                errorLog("Chunk ${chunk.chunkIndex} failed (attempt ${attempt + 1}): ${e.message}")
                lastError = e
                attempt++
            }
        }

        onChunkProgress(chunk.id, offset, DownloadChunkStatus.FAILED)
        return ChunkOutcome.Failed(lastError)
    }

    /** Returns `true` if the chunk's full range was written, `false` if interrupted (paused/stopped) partway. */
    private suspend fun downloadChunkOnce(
        chunk: DownloadChunk,
        startOffset: Long,
        url: String,
        headers: Map<String, String>,
        destinationFile: UniFile,
        shouldInterrupt: () -> Boolean,
        onBytesWritten: suspend (Long) -> Unit,
    ): Boolean {
        val isOpenEnded = chunk.rangeEnd < 0
        val rangeStart = chunk.rangeStart + startOffset
        val rangeHeader = if (isOpenEnded) "bytes=$rangeStart-" else "bytes=$rangeStart-${chunk.rangeEnd}"
        val expectedBytes = if (isOpenEnded) null else chunk.rangeEnd - chunk.rangeStart + 1

        val request = okRequest(url, headers) { addHeader("Range", rangeHeader) }

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Chunk request failed: ${response.code}")
            }

            // A server that ignores Range answers 200 with the entire file rather than 206 with the
            // slice asked for. Writing that body at this chunk's offset — which is exactly what the
            // old `isSuccessful`-only check allowed — silently produces a large, plausible, corrupt
            // file that the minimum-size sanity check can't catch. Only the chunk starting at zero
            // can safely take a whole-file response, and only if it's the only chunk.
            val isWholeFileResponse = response.code == HTTP_OK
            if (isWholeFileResponse && (rangeStart > 0L || !isOpenEnded)) {
                throw RangeUnsupportedException(url)
            }

            // Concurrent chunks write to disjoint byte ranges of the same destination file, so
            // this needs true random access — unlike the HLS engine, an append-only OutputStream
            // won't do. UniFile.createRandomAccessFile() would work, but it needs a reflection
            // trick to get seekable access to a SAF-backed file that can fail outright on some
            // devices. ParcelFileDescriptor gives the same seek+write semantics through a real,
            // non-reflective file descriptor instead.
            val pfd = context.contentResolver.openFileDescriptor(destinationFile.uri, "rw")
                ?: throw IOException("Failed to open ${destinationFile.uri} for writing")

            pfd.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).channel.use { channel ->
                    channel.position(rangeStart)

                    val source = response.body.source()
                    val buffer = ByteArray(TRANSFER_BUFFER_BYTES)
                    var written = startOffset

                    while (true) {
                        if (shouldInterrupt()) return false

                        val read = source.read(buffer)
                        if (read == -1) {
                            // A 200/206 response can still end early on a flaky connection or a
                            // misbehaving CDN; treat under-delivery as a failure so it retries
                            // instead of silently completing with a truncated file.
                            if (expectedBytes != null && written < expectedBytes) {
                                throw IOException(
                                    "Chunk ${chunk.chunkIndex} under-delivered: expected $expectedBytes bytes, got $written"
                                )
                            }
                            return true
                        }

                        val byteBuffer = ByteBuffer.wrap(buffer, 0, read)
                        while (byteBuffer.hasRemaining()) {
                            channel.write(byteBuffer)
                        }
                        written += read
                        onBytesWritten(written)
                    }
                }
            }
        }
    }

    private sealed interface ChunkOutcome {
        data object Completed : ChunkOutcome

        data object Interrupted : ChunkOutcome

        data class Failed(
            val cause: Throwable,
        ) : ChunkOutcome
    }

    companion object {
        private const val MAX_CHUNK_RETRIES = 3
        private const val TRANSFER_BUFFER_BYTES = 16 * 1024
        private const val HTTP_OK = 200
    }
}
