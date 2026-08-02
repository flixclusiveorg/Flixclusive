package com.flixclusive.core.network.download.impl

import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.util.log.LogRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

class LinkProbeImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var server: MockWebServer
    private lateinit var linkProbe: LinkProbeImpl

    @get:Rule
    val logRule = LogRule()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        linkProbe = LinkProbeImpl(
            client = OkHttpClient(),
            appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `probe should return reachable result with content length on success`() =
        runTest(testDispatcher) {
            val body = "a".repeat(1024)
            server.enqueue(MockResponse().setBody(body))

            val result = linkProbe.probe(server.url("/file.mp4").toString())

            expectThat(result.isReachable).isTrue()
            expectThat(result.contentLength).isEqualTo(1024L)
            expectThat(result.bytesPerSecond).isNotNull()
        }

    @Test
    fun `probe should return unreachable result on non-2xx response`() =
        runTest(testDispatcher) {
            server.enqueue(MockResponse().setResponseCode(404))

            val result = linkProbe.probe(server.url("/missing.mp4").toString())

            expectThat(result.isReachable).isFalse()
            expectThat(result.contentLength).isNull()
            expectThat(result.bytesPerSecond).isNull()
        }

    @Test
    fun `probe should return unreachable result when the connection is dropped`() =
        runTest(testDispatcher) {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

            val result = linkProbe.probe(server.url("/broken.mp4").toString())

            expectThat(result.isReachable).isFalse()
        }

    @Test
    fun `probe should detect HLS via a mpegurl content type even without the m3u8 extension`() =
        runTest(testDispatcher) {
            server.enqueue(
                MockResponse()
                    .setBody("#EXTM3U\n#EXT-X-VERSION:3\n")
                    .setHeader("Content-Type", "application/vnd.apple.mpegurl; charset=utf-8")
            )

            val result = linkProbe.probe(server.url("/playlist").toString())

            expectThat(result.isHls).isTrue()
        }

    @Test
    fun `probe should detect HLS via the EXTM3U header when content type is generic`() =
        runTest(testDispatcher) {
            server.enqueue(
                MockResponse()
                    .setBody("#EXTM3U\n#EXT-X-VERSION:3\n")
                    .setHeader("Content-Type", "text/plain")
            )

            val result = linkProbe.probe(server.url("/playlist").toString())

            expectThat(result.isHls).isTrue()
        }

    @Test
    fun `probe should not flag a regular video file as HLS`() =
        runTest(testDispatcher) {
            server.enqueue(MockResponse().setBody("a".repeat(1024)).setHeader("Content-Type", "video/mp4"))

            val result = linkProbe.probe(server.url("/file.mp4").toString())

            expectThat(result.isHls).isFalse()
        }

    @Test
    fun `probe should forward custom headers to the request`() =
        runTest(testDispatcher) {
            server.enqueue(MockResponse().setBody("body"))

            linkProbe.probe(
                url = server.url("/file.mp4").toString(),
                headers = mapOf("Authorization" to "Bearer test-token"),
            )

            val recordedRequest = server.takeRequest()
            expectThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-token")
        }
}
