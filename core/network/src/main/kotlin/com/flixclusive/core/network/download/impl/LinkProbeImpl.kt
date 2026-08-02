package com.flixclusive.core.network.download.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.download.LinkProbe
import com.flixclusive.core.network.download.LinkProbeResult
import com.flixclusive.core.util.log.errorLog
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

internal class LinkProbeImpl @Inject constructor(
    client: OkHttpClient,
    private val appDispatchers: AppDispatchers,
) : LinkProbe {
    private val client by lazy {
        client
            .newBuilder()
            .cache(null)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    override suspend fun probe(
        url: String,
        headers: Map<String, String>,
    ): LinkProbeResult =
        withContext(appDispatchers.io) {
            withTimeoutOrNull(PROBE_TIMEOUT_MS) { runProbe(url, headers) } ?: UNREACHABLE_RESULT
        }

    private fun runProbe(
        url: String,
        headers: Map<String, String>,
    ): LinkProbeResult {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (name, value) -> requestBuilder.addHeader(name, value) }

        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return UNREACHABLE_RESULT

                val contentLength = response.body.contentLength().takeIf { it >= 0 }
                val bytesPerSecond = measureThroughputBytesPerSecond(response)

                LinkProbeResult(
                    isReachable = bytesPerSecond != null,
                    contentLength = contentLength,
                    bytesPerSecond = bytesPerSecond,
                )
            }
        } catch (e: Throwable) {
            errorLog("Link probe failed for $url: ${e.message}")
            UNREACHABLE_RESULT
        }
    }

    private fun measureThroughputBytesPerSecond(response: Response): Long? {
        val source = response.body.source()
        val buffer = ByteArray(PROBE_READ_CHUNK_BYTES)
        var bytesRead = 0L
        val startTimeNs = System.nanoTime()

        // Bounded sample read (size- or time-capped, whichever hits first) to estimate
        // throughput without pulling down the whole file just to rank candidate links.
        while (bytesRead < PROBE_SAMPLE_BYTES) {
            val elapsedMs = (System.nanoTime() - startTimeNs) / 1_000_000
            if (elapsedMs >= PROBE_SAMPLE_DURATION_MS) break

            val read = source.read(buffer)
            if (read == -1) break
            bytesRead += read
        }

        if (bytesRead == 0L) return null

        val elapsedSeconds = ((System.nanoTime() - startTimeNs) / 1_000_000_000.0).coerceAtLeast(MIN_ELAPSED_SECONDS)
        return (bytesRead / elapsedSeconds).toLong()
    }

    companion object {
        private const val PROBE_TIMEOUT_MS = 5000L
        private const val PROBE_SAMPLE_DURATION_MS = 1500L
        private const val PROBE_SAMPLE_BYTES = 256 * 1024L
        private const val PROBE_READ_CHUNK_BYTES = 8192
        private const val MIN_ELAPSED_SECONDS = 0.05

        private val UNREACHABLE_RESULT =
            LinkProbeResult(isReachable = false, contentLength = null, bytesPerSecond = null)
    }
}
