package com.flixclusive.data.downloads.hls.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.downloads.hls.HlsSegmentDecryptor
import com.flixclusive.data.downloads.hls.HlsSegmentInfo
import com.flixclusive.data.downloads.hls.HlsTransferEngine
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.hippo.unifile.UniFile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class HlsTransferEngineImpl @Inject constructor(
    client: OkHttpClient,
    private val appDispatchers: AppDispatchers,
) : HlsTransferEngine {
    private val client by lazy {
        client
            .newBuilder()
            .cache(null)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

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

            val randomAccessFile = destinationFile.createRandomAccessFile("rw")
            try {
                // Segments have no known byte length ahead of time, so resuming can't seek to a
                // specific offset the way the byte-range engine does — appending at the file's
                // current end is correct as long as writes only ever happen in segment order,
                // which the pending-buffer below guarantees.
                randomAccessFile.seek(destinationFile.length())

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

                                val bytes = fetchSegmentWithRetries(segments[index], headers, keyCache)
                                if (bytes == null) {
                                    fileMutex.withLock { failed = true }
                                    return@launch
                                }

                                val writtenSoFar = fileMutex.withLock {
                                    if (failed || interrupted) return@withLock nextWriteIndex

                                    if (index == nextWriteIndex) {
                                        randomAccessFile.write(bytes)
                                        nextWriteIndex++

                                        while (true) {
                                            val cached = pendingData.remove(nextWriteIndex) ?: break
                                            randomAccessFile.write(cached)
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
                randomAccessFile.close()
            }

            when {
                interrupted -> MediaTransferResult.Cancelled
                failed -> MediaTransferResult.Failed(IOException("One or more HLS segments failed to download"))
                else -> MediaTransferResult.Completed
            }
        }

    private suspend fun fetchSegmentWithRetries(
        segment: HlsSegmentInfo,
        headers: Map<String, String>,
        keyCache: ConcurrentHashMap<String, ByteArray>,
    ): ByteArray? {
        repeat(MAX_SEGMENT_RETRIES) { attempt ->
            try {
                return downloadAndDecrypt(segment, headers, keyCache)
            } catch (e: Throwable) {
                errorLog("HLS segment fetch failed (attempt ${attempt + 1}): ${e.message}")
            }
        }
        return null
    }

    private fun downloadAndDecrypt(
        segment: HlsSegmentInfo,
        headers: Map<String, String>,
        keyCache: ConcurrentHashMap<String, ByteArray>,
    ): ByteArray {
        val requestBuilder = Request.Builder().url(segment.url)
        headers.forEach { (name, value) -> requestBuilder.addHeader(name, value) }
        if (segment.byteRangeLength >= 0) {
            val rangeEnd = segment.byteRangeOffset + segment.byteRangeLength - 1
            requestBuilder.addHeader("Range", "bytes=${segment.byteRangeOffset}-$rangeEnd")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Segment request failed: ${response.code}")

            val bytes = response.body.bytes()
            if (bytes.isEmpty()) throw IOException("Segment returned no data")

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
            val requestBuilder = Request.Builder().url(keyUri)
            headers.forEach { (name, value) -> requestBuilder.addHeader(name, value) }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to fetch encryption key: ${response.code}")
                response.body.bytes()
            }
        }

    companion object {
        private const val HLS_PARALLEL_CONNECTIONS = 3
        private const val MAX_SEGMENT_RETRIES = 3
    }
}
