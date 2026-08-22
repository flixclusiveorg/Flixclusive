package com.flixclusive.data.downloads.hls.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.downloads.hls.HlsSegmentDecryptor
import com.flixclusive.data.downloads.hls.HlsSegmentInfo
import com.flixclusive.data.downloads.hls.HlsTransferEngine
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.hippo.unifile.UniFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.flixclusive.data.downloads.di.DownloadHttpClient
import com.flixclusive.data.downloads.util.HlsSignature
import com.flixclusive.data.downloads.util.okRequest
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class HlsTransferEngineImpl @Inject constructor(
    @param:DownloadHttpClient private val client: OkHttpClient,
    private val appDispatchers: AppDispatchers,
) : HlsTransferEngine {
    override suspend fun transfer(
        segments: List<HlsSegmentInfo>,
        startIndex: Int,
        headers: Map<String, String>,
        destinationFile: UniFile,
        shouldInterrupt: () -> Boolean,
        onSegmentWritten: suspend (Int, Int) -> Unit,
    ): MediaTransferResult =
        withContext(appDispatchers.io) {
            if (startIndex >= segments.size) return@withContext MediaTransferResult.Completed

            val keyCache = ConcurrentHashMap<String, ByteArray>()
            val currentIndexMutex = Mutex()
            val currentIndexIterator = (startIndex until segments.size).iterator()
            val fileMutex = Mutex()
            val pendingData = HashMap<Int, ByteArray>()
            var nextWriteIndex = startIndex
            var interrupted = false
            var failed = false

            // Written from several workers, read once they've all finished; guarded by fileMutex
            // alongside the other shared flags.
            var lastSegmentError: Throwable? = null

            // Segments have no known byte length ahead of time, so resuming can't seek to a
            // specific offset the way the byte-range engine does — appending at the file's
            // current end is correct as long as writes only ever happen in segment order, which
            // the pending-buffer below guarantees. An append-mode OutputStream (rather than
            // UniFile's createRandomAccessFile, which needs a reflection trick to get seekable
            // access to a SAF-backed file and can fail outright on some devices) is exactly what
            // that write pattern needs, and every device supports it natively.
            val outputStream = destinationFile.openOutputStream(true)
            try {
                coroutineScope {
                    val workerCount = HLS_PARALLEL_CONNECTIONS.coerceAtMost(segments.size - startIndex)
                    repeat(workerCount) {
                        launch {
                            while (true) {
                                if (shouldInterrupt()) {
                                    fileMutex.withLock { interrupted = true }
                                    return@launch
                                }

                                val index = currentIndexMutex.withLock {
                                    if (!currentIndexIterator.hasNext()) return@launch
                                    currentIndexIterator.nextInt()
                                }

                                var segmentError: Throwable? = null
                                val bytes = fetchSegmentWithRetries(segments[index], headers, keyCache) {
                                    segmentError = it
                                }
                                if (bytes == null) {
                                    fileMutex.withLock {
                                        failed = true
                                        lastSegmentError = segmentError ?: lastSegmentError
                                    }
                                    return@launch
                                }

                                val writtenSoFar = fileMutex.withLock {
                                    if (failed || interrupted) return@withLock nextWriteIndex

                                    if (index == nextWriteIndex) {
                                        outputStream.write(bytes)
                                        nextWriteIndex++

                                        while (true) {
                                            val cached = pendingData.remove(nextWriteIndex) ?: break
                                            outputStream.write(cached)
                                            nextWriteIndex++
                                        }
                                    } else {
                                        pendingData[index] = bytes
                                    }

                                    nextWriteIndex
                                }

                                onSegmentWritten(writtenSoFar, segments.size)
                            }
                        }
                    }
                }
            } finally {
                outputStream.close()
            }

            when {
                interrupted -> MediaTransferResult.Cancelled
                // The real cause where we have one, so the caller can tell a dead link apart from a
                // full disk or a dropped connection rather than blaming the link for everything.
                failed -> MediaTransferResult.Failed(
                    lastSegmentError ?: IOException("One or more HLS segments failed to download"),
                )
                nextWriteIndex != segments.size -> MediaTransferResult.Failed(
                    IOException("HLS transfer wrote $nextWriteIndex of ${segments.size} segments"),
                )
                else -> MediaTransferResult.Completed
            }
        }

    private suspend fun fetchSegmentWithRetries(
        segment: HlsSegmentInfo,
        headers: Map<String, String>,
        keyCache: ConcurrentHashMap<String, ByteArray>,
        onError: (Throwable) -> Unit,
    ): ByteArray? {
        repeat(MAX_SEGMENT_RETRIES) { attempt ->
            try {
                return downloadAndDecrypt(segment, headers, keyCache)
            } catch (e: CancellationException) {
                // Never retry a torn-down scope.
                throw e
            } catch (e: Throwable) {
                errorLog("HLS segment fetch failed (attempt ${attempt + 1}): ${e.message}")
                onError(e)
            }
        }
        return null
    }

    private fun downloadAndDecrypt(
        segment: HlsSegmentInfo,
        headers: Map<String, String>,
        keyCache: ConcurrentHashMap<String, ByteArray>,
    ): ByteArray {
        val request = okRequest(segment.url, headers) {
            if (segment.byteRangeLength >= 0) {
                val rangeEnd = segment.byteRangeOffset + segment.byteRangeLength - 1
                addHeader("Range", "bytes=${segment.byteRangeOffset}-$rangeEnd")
            }
        }

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Segment request failed: ${response.code}")

            val bytes = response.body.bytes()
            if (bytes.isEmpty()) throw IOException("Segment returned no data")
            if (bytes.size < MIN_MEDIA_SEGMENT_BYTES && bytes.all { it >= 0 }) {
                throw IOException("Segment returned ${bytes.size} bytes of text, not media")
            }
            if (HlsSignature.matchesBody(bytes)) {
                throw IOException("Segment URL returned a playlist, not media")
            }

            val keyUri = segment.encryptionKeyUri ?: return bytes
            val iv = segment.encryptionIv ?: throw IOException("Encrypted segment is missing its IV")
            val key = fetchEncryptionKey(keyUri, headers, keyCache)
            return HlsSegmentDecryptor.decrypt(key, HlsSegmentDecryptor.parseIv(iv), bytes)
        }
    }

    private fun fetchEncryptionKey(
        keyUri: String,
        headers: Map<String, String>,
        keyCache: ConcurrentHashMap<String, ByteArray>,
    ): ByteArray =
        keyCache.getOrPut(keyUri) {
            val request = okRequest(keyUri, headers)

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to fetch encryption key: ${response.code}")
                response.body.bytes()
            }
        }

    companion object {
        private const val HLS_PARALLEL_CONNECTIONS = 3
        private const val MAX_SEGMENT_RETRIES = 3
        private const val MIN_MEDIA_SEGMENT_BYTES = 128
    }
}
