package com.flixclusive.data.downloads.probe.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.data.downloads.probe.LinkProbe
import com.flixclusive.data.downloads.probe.LinkProbeResult
import com.flixclusive.core.util.log.errorLog
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.flixclusive.data.downloads.di.DownloadHttpClient
import com.flixclusive.data.downloads.util.HlsSignature
import com.flixclusive.data.downloads.util.okRequest
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

internal class LinkProbeImpl @Inject constructor(
    @param:DownloadHttpClient private val client: OkHttpClient,
    private val appDispatchers: AppDispatchers,
) : LinkProbe {
    override suspend fun probe(
        url: String,
        headers: Map<String, String>,
    ): LinkProbeResult =
        withContext(appDispatchers.io) {
            withTimeoutOrNull(PROBE_TIMEOUT_MS.milliseconds) { runProbe(url, headers) } ?: UNREACHABLE_RESULT
        }

    private fun runProbe(
        url: String,
        headers: Map<String, String>,
    ): LinkProbeResult {
        val request = okRequest(url, headers)

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return UNREACHABLE_RESULT

                val contentLength = response.body.contentLength().takeIf { it >= 0 }
                val sample = readThroughputSample(response)
                val isHls = isHlsContent(url, response.header("Content-Type"), sample.head)

                LinkProbeResult(
                    isReachable = sample.bytesPerSecond != null,
                    contentLength = contentLength,
                    bytesPerSecond = sample.bytesPerSecond,
                    isHls = isHls,
                )
            }
        } catch (e: Throwable) {
            errorLog("Link probe failed for $url: ${e.message}")
            UNREACHABLE_RESULT
        }
    }

    private class ThroughputSample(
        val bytesPerSecond: Long?,
        val head: ByteArray,
    )

    private fun readThroughputSample(response: Response): ThroughputSample {
        val source = response.body.source()
        val buffer = ByteArray(PROBE_READ_CHUNK_BYTES)
        var bytesRead = 0L
        val head = ByteArrayOutputStream(SNIFF_BYTES)
        val startTimeNs = System.nanoTime()

        // Bounded sample read (size- or time-capped, whichever hits first) to estimate
        // throughput without pulling down the whole file just to rank candidate links. The
        // leading bytes double as the sample checked for an HLS manifest signature, so
        // detection costs no extra request.
        while (bytesRead < PROBE_SAMPLE_BYTES) {
            val elapsedMs = (System.nanoTime() - startTimeNs) / 1_000_000
            if (elapsedMs >= PROBE_SAMPLE_DURATION_MS) break

            val read = source.read(buffer)
            if (read == -1) break
            if (head.size() < SNIFF_BYTES) {
                head.write(buffer, 0, minOf(read, SNIFF_BYTES - head.size()))
            }
            bytesRead += read
        }

        if (bytesRead == 0L) return ThroughputSample(bytesPerSecond = null, head = head.toByteArray())

        val elapsedSeconds = ((System.nanoTime() - startTimeNs) / 1_000_000_000.0).coerceAtLeast(MIN_ELAPSED_SECONDS)
        return ThroughputSample(
            bytesPerSecond = (bytesRead / elapsedSeconds).toLong(),
            head = head.toByteArray(),
        )
    }

    private fun isHlsContent(
        url: String,
        contentType: String?,
        head: ByteArray,
    ): Boolean =
        HlsSignature.matchesUrl(url) ||
            HlsSignature.matchesContentType(contentType) ||
            HlsSignature.matchesBody(head)

    companion object {
        private const val PROBE_TIMEOUT_MS = 5000L
        private const val PROBE_SAMPLE_DURATION_MS = 1500L
        private const val PROBE_SAMPLE_BYTES = 256 * 1024L
        private const val PROBE_READ_CHUNK_BYTES = 8192
        private const val SNIFF_BYTES = 1024
        private const val MIN_ELAPSED_SECONDS = 0.05

        private val UNREACHABLE_RESULT =
            LinkProbeResult(isReachable = false, contentLength = null, bytesPerSecond = null)
    }
}
