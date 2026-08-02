package com.flixclusive.data.downloads.transfer.impl

import com.flixclusive.core.database.entity.downloads.DownloadChunk
import com.flixclusive.core.database.entity.downloads.DownloadChunkStatus
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.util.log.LogRule
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
import java.io.RandomAccessFile

class MediaTransferEngineImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var server: MockWebServer
    private lateinit var engine: MediaTransferEngineImpl
    private lateinit var destinationFile: File

    @get:Rule
    val logRule = LogRule()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        engine = MediaTransferEngineImpl(
            client = OkHttpClient(),
            appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
        )

        destinationFile = File.createTempFile("media-transfer-test", ".tmp")
        destinationFile.deleteOnExit()
    }

    @After
    fun tearDown() {
        server.shutdown()
        destinationFile.delete()
    }

    private fun chunk(
        id: Long = 1,
        chunkIndex: Int = 0,
        rangeStart: Long = 0,
        rangeEnd: Long = -1,
        bytesDownloaded: Long = 0,
    ) = DownloadChunk(
        id = id,
        downloadItemId = 1,
        chunkIndex = chunkIndex,
        rangeStart = rangeStart,
        rangeEnd = rangeEnd,
        bytesDownloaded = bytesDownloaded,
    )

    @Test
    fun `transfer should download a single chunk fully and write it to the destination file`() =
        runTest(testDispatcher) {
            val content = "hello world".repeat(100)
            server.enqueue(MockResponse().setBody(content))

            val progressEvents = mutableListOf<DownloadChunkStatus>()
            val result = engine.transfer(
                chunks = listOf(chunk(rangeEnd = content.length - 1L)),
                url = server.url("/file.mp4").toString(),
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _, status -> progressEvents += status }

            expectThat(result).isA<MediaTransferResult.Completed>()
            expectThat(destinationFile.readText()).isEqualTo(content)
            expectThat(progressEvents.last()).isEqualTo(DownloadChunkStatus.COMPLETED)
        }

    @Test
    fun `transfer should stop early and report Cancelled when interrupted mid-download`() =
        runTest(testDispatcher) {
            val content = "x".repeat(50_000)
            server.enqueue(MockResponse().setBody(content))

            var bytesSeen = 0L
            val result = engine.transfer(
                chunks = listOf(chunk(rangeEnd = content.length - 1L)),
                url = server.url("/file.mp4").toString(),
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { bytesSeen > 1000 },
            ) { _, bytesDownloaded, _ -> bytesSeen = bytesDownloaded }

            expectThat(result).isA<MediaTransferResult.Cancelled>()
        }

    @Test
    fun `transfer should resume from the chunk's persisted offset using a Range header`() =
        runTest(testDispatcher) {
            val fullContent = "0123456789".repeat(20)
            val alreadyDownloaded = 50L
            RandomAccessFile(
                destinationFile,
                "rw"
            ).use { it.write(fullContent.take(alreadyDownloaded.toInt()).toByteArray()) }

            server.enqueue(MockResponse().setBody(fullContent.substring(alreadyDownloaded.toInt())))

            val result = engine.transfer(
                chunks = listOf(chunk(rangeEnd = fullContent.length - 1L, bytesDownloaded = alreadyDownloaded)),
                url = server.url("/file.mp4").toString(),
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _, _ -> }

            expectThat(result).isA<MediaTransferResult.Completed>()
            expectThat(destinationFile.readText()).isEqualTo(fullContent)

            val recordedRequest = server.takeRequest()
            expectThat(recordedRequest.getHeader("Range")).isEqualTo("bytes=50-${fullContent.length - 1}")
        }

    @Test
    fun `transfer should report Failed when a chunk exhausts its retries`() =
        runTest(testDispatcher) {
            repeat(3) { server.enqueue(MockResponse().setResponseCode(500)) }

            val progressEvents = mutableListOf<DownloadChunkStatus>()
            val result = engine.transfer(
                chunks = listOf(chunk(rangeEnd = 99)),
                url = server.url("/file.mp4").toString(),
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _, status -> progressEvents += status }

            expectThat(result).isA<MediaTransferResult.Failed>()
            expectThat(progressEvents.last()).isEqualTo(DownloadChunkStatus.FAILED)
        }

    @Test
    fun `transfer should download multiple chunks in parallel into their own byte ranges`() =
        runTest(testDispatcher) {
            val fullContent = ('a'..'z').joinToString("").repeat(400)
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val range = request.getHeader("Range")!!.removePrefix("bytes=")
                    val (start, end) = range.split("-").map { it.toInt() }
                    return MockResponse().setBody(fullContent.substring(start, end + 1))
                }
            }

            val chunks = listOf(
                chunk(id = 1, chunkIndex = 0, rangeStart = 0, rangeEnd = (fullContent.length / 2 - 1).toLong()),
                chunk(
                    id = 2,
                    chunkIndex = 1,
                    rangeStart = (fullContent.length / 2).toLong(),
                    rangeEnd =
                        fullContent.length - 1L
                ),
            )

            val result = engine.transfer(
                chunks = chunks,
                url = server.url("/file.mp4").toString(),
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _, _ -> }

            expectThat(result).isA<MediaTransferResult.Completed>()
            expectThat(destinationFile.readText()).isEqualTo(fullContent)
        }

    @Test
    fun `transfer should report Failed when a chunk under-delivers fewer bytes than its declared range`() =
        runTest(testDispatcher) {
            // Every attempt (including retries, which resume from the offset already written)
            // delivers only 10 more bytes, so the cumulative total across all 3 attempts (30
            // bytes) still falls well short of the declared 1000-byte range.
            repeat(3) { server.enqueue(MockResponse().setBody("x".repeat(10))) }

            val progressEvents = mutableListOf<DownloadChunkStatus>()
            val result = engine.transfer(
                chunks = listOf(chunk(rangeEnd = 999)),
                url = server.url("/file.mp4").toString(),
                headers = emptyMap(),
                destinationFile = UniFile.fromFile(destinationFile)!!,
                shouldInterrupt = { false },
            ) { _, _, status -> progressEvents += status }

            expectThat(result).isA<MediaTransferResult.Failed>()
            expectThat(progressEvents.last()).isEqualTo(DownloadChunkStatus.FAILED)
        }
}
