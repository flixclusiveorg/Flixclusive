package com.flixclusive.data.downloads.hls.impl

import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.data.downloads.hls.HlsSegmentInfo
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.hippo.unifile.UniFile
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class HlsTransferEngineImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var server: MockWebServer
    private lateinit var engine: HlsTransferEngineImpl
    private lateinit var destinationFile: File

    @get:Rule
    val logRule = LogRule()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        engine = HlsTransferEngineImpl(
            client = OkHttpClient(),
            appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
        )

        destinationFile = File.createTempFile("hls-transfer-test", ".tmp")
        destinationFile.deleteOnExit()
    }

    @After
    fun tearDown() {
        server.shutdown()
        destinationFile.delete()
    }

    private fun segment(
        path: String,
        byteRangeOffset: Long = 0,
        byteRangeLength: Long = -1,
        encryptionKeyUri: String? = null,
        encryptionIv: String? = null,
    ) = HlsSegmentInfo(
        url = server.url(path).toString(),
        byteRangeOffset = byteRangeOffset,
        byteRangeLength = byteRangeLength,
        encryptionKeyUri = encryptionKeyUri,
        encryptionIv = encryptionIv,
    )

    private fun body(marker: String) = marker.repeat(SEGMENT_BODY_LENGTH)

    @Test
    fun `transfer should download all segments in order into the destination file`() =
        runTest(testDispatcher) {
            server.enqueue(MockResponse().setBody(body("A")))
            server.enqueue(MockResponse().setBody(body("B")))
            server.enqueue(MockResponse().setBody(body("C")))

            val result = engine.transfer(
                segments = listOf(segment("/0.ts"), segment("/1.ts"), segment("/2.ts")),
                startIndex = 0,
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _ -> }

            expectThat(result).isA<MediaTransferResult.Completed>()
            expectThat(destinationFile.readText()).isEqualTo(body("A") + body("B") + body("C"))
        }

    @Test
    fun `transfer should resume from startIndex leaving the earlier bytes untouched`() =
        runTest(testDispatcher) {
            destinationFile.writeText(body("A"))
            server.enqueue(MockResponse().setBody(body("B")))

            val result = engine.transfer(
                segments = listOf(segment("/0.ts"), segment("/1.ts")),
                startIndex = 1,
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _ -> }

            expectThat(result).isA<MediaTransferResult.Completed>()
            expectThat(destinationFile.readText()).isEqualTo(body("A") + body("B"))
            expectThat(server.requestCount).isEqualTo(1)
        }

    @Test
    fun `transfer should write out-of-order segments in the correct order`() =
        runTest(testDispatcher) {
            val bodies = mapOf("/0.ts" to body("A"), "/1.ts" to body("B"), "/2.ts" to body("C"))
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    // segment 0 resolves last, forcing 1 and 2 to buffer in the pending map
                    if (request.path == "/0.ts") Thread.sleep(200)
                    return MockResponse().setBody(bodies.getValue(request.path!!))
                }
            }

            val result = engine.transfer(
                segments = listOf(segment("/0.ts"), segment("/1.ts"), segment("/2.ts")),
                startIndex = 0,
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _ -> }

            expectThat(result).isA<MediaTransferResult.Completed>()
            expectThat(destinationFile.readText()).isEqualTo(body("A") + body("B") + body("C"))
        }

    @Test
    fun `transfer should stop early and report Cancelled when interrupted`() =
        runTest(testDispatcher) {
            repeat(5) { server.enqueue(MockResponse().setBody("x".repeat(1000))) }

            var segmentsWritten = 0
            val result = engine.transfer(
                segments = List(5) { segment("/$it.ts") },
                startIndex = 0,
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { segmentsWritten >= 1 },
            ) { written, _ -> segmentsWritten = written }

            expectThat(result).isA<MediaTransferResult.Cancelled>()
        }

    @Test
    fun `transfer should report Failed when a segment exhausts its retries`() =
        runTest(testDispatcher) {
            repeat(3) { server.enqueue(MockResponse().setResponseCode(500)) }

            val result = engine.transfer(
                segments = listOf(segment("/0.ts")),
                startIndex = 0,
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _ -> }

            expectThat(result).isA<MediaTransferResult.Failed>()
        }

    @Test
    fun `transfer should report Failed when a segment answers 200 with a short text body`() =
        runTest(testDispatcher) {
            repeat(3) { server.enqueue(MockResponse().setBody("Not found")) }

            val result = engine.transfer(
                segments = listOf(segment("/0.ts")),
                startIndex = 0,
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _ -> }

            expectThat(result).isA<MediaTransferResult.Failed>()
            expectThat(destinationFile.length()).isEqualTo(0)
        }

    @Test
    fun `transfer should report Failed when a segment answers with a playlist`() =
        runTest(testDispatcher) {
            val playlist = "#EXTM3U\n" + "#EXTINF:4.0,\n/0.ts\n".repeat(20)
            repeat(3) { server.enqueue(MockResponse().setBody(playlist)) }

            val result = engine.transfer(
                segments = listOf(segment("/0.ts")),
                startIndex = 0,
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _ -> }

            expectThat(result).isA<MediaTransferResult.Failed>()
            expectThat(destinationFile.length()).isEqualTo(0)
        }

    @Test
    fun `transfer should accept a short segment that is binary rather than text`() =
        runTest(testDispatcher) {
            val binary = ByteArray(16) { 0x80.toByte() }
            server.enqueue(MockResponse().setBody(okio.Buffer().write(binary)))

            val result = engine.transfer(
                segments = listOf(segment("/0.ts")),
                startIndex = 0,
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _ -> }

            expectThat(result).isA<MediaTransferResult.Completed>()
            expectThat(destinationFile.readBytes().toList()).isEqualTo(binary.toList())
        }

    @Test
    fun `transfer should send a Range header for byte-range segments`() =
        runTest(testDispatcher) {
            server.enqueue(MockResponse().setBody(body("A")))

            engine.transfer(
                segments = listOf(segment("/combined.ts", byteRangeOffset = 100, byteRangeLength = 4)),
                startIndex = 0,
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _ -> }

            expectThat(server.takeRequest().getHeader("Range")).isEqualTo("bytes=100-103")
        }

    @Test
    fun `transfer should decrypt AES-128 encrypted segments before writing`() =
        runTest(testDispatcher) {
            val plaintext = body("z")
            val key = ByteArray(16) { 1 }
            val iv = ByteArray(16) { 2 }
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            val ciphertext = cipher.doFinal(plaintext.toByteArray())

            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                    "/key" -> MockResponse().setBody(okio.Buffer().write(key))
                    else -> MockResponse().setBody(okio.Buffer().write(ciphertext))
                }
            }

            val ivHex = iv.joinToString("") { "%02x".format(it) }
            val result = engine.transfer(
                segments = listOf(
                    segment("/0.ts", encryptionKeyUri = server.url("/key").toString(), encryptionIv = "0x$ivHex")
                ),
                startIndex = 0,
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _ -> }

            expectThat(result).isA<MediaTransferResult.Completed>()
            expectThat(destinationFile.readText()).isEqualTo(plaintext)
        }

    private companion object {
        const val SEGMENT_BODY_LENGTH = 200
    }
}
