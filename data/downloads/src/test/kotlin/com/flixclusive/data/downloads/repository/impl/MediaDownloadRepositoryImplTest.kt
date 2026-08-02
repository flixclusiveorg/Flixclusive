package com.flixclusive.data.downloads.repository.impl

import com.flixclusive.core.database.dao.downloads.DownloadChunkDao
import com.flixclusive.core.database.dao.downloads.DownloadItemDao
import com.flixclusive.core.database.entity.downloads.DownloadChunk
import com.flixclusive.core.database.entity.downloads.DownloadChunkStatus
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.core.database.entity.downloads.DownloadStreamCandidate
import com.flixclusive.data.downloads.hls.HlsSegmentInfo
import com.flixclusive.data.downloads.hls.HlsTransferEngine
import com.flixclusive.data.downloads.model.DownloadInterruptReason
import com.flixclusive.data.downloads.transfer.MediaTransferEngine
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.hippo.unifile.UniFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import com.flixclusive.model.media.common.MediaType
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class MediaDownloadRepositoryImplTest {
    private lateinit var downloadItemDao: DownloadItemDao
    private lateinit var downloadChunkDao: DownloadChunkDao
    private lateinit var mediaTransferEngine: MediaTransferEngine
    private lateinit var hlsTransferEngine: HlsTransferEngine
    private lateinit var repository: MediaDownloadRepositoryImpl

    private val destinationFile = mockk<UniFile>()

    @Before
    fun setup() {
        downloadItemDao = mockk(relaxed = true)
        downloadChunkDao = mockk(relaxed = true)
        mediaTransferEngine = mockk()
        hlsTransferEngine = mockk()

        repository = MediaDownloadRepositoryImpl(
            downloadItemDao,
            downloadChunkDao,
            mediaTransferEngine,
            hlsTransferEngine,
        )
    }

    @Test
    fun `runTransfer should plan and insert chunks when none exist yet`() =
        runTest {
            coEvery { downloadChunkDao.getChunksForItem(1) } returnsMany listOf(emptyList(), emptyList())
            coEvery {
                mediaTransferEngine.transfer(any(), any(), any(), any(), any(), any())
            } returns MediaTransferResult.Completed

            repository.runTransfer(
                1,
                DownloadPhase.STREAM,
                "https://example.com/file",
                emptyMap(),
                destinationFile,
                10L * 1024 * 1024
            )

            coVerify(exactly = 1) { downloadChunkDao.insertAll(any()) }
        }

    @Test
    fun `runTransfer should reuse existing chunks instead of replanning`() =
        runTest {
            val existing =
                listOf(DownloadChunk(id = 1, downloadItemId = 1, chunkIndex = 0, rangeStart = 0, rangeEnd = 999))
            coEvery { downloadChunkDao.getChunksForItem(1) } returns existing
            coEvery {
                mediaTransferEngine.transfer(any(), any(), any(), any(), any(), any())
            } returns MediaTransferResult.Completed

            repository.runTransfer(
                1,
                DownloadPhase.STREAM,
                "https://example.com/file",
                emptyMap(),
                destinationFile,
                1000L
            )

            coVerify(exactly = 0) { downloadChunkDao.insertAll(any()) }
        }

    @Test
    fun `runTransfer should pass persisted chunks to the transfer engine`() =
        runTest {
            val existing =
                listOf(DownloadChunk(id = 5, downloadItemId = 1, chunkIndex = 0, rangeStart = 0, rangeEnd = 999))
            coEvery { downloadChunkDao.getChunksForItem(1) } returns existing
            val chunksSlot = slot<List<DownloadChunk>>()
            coEvery {
                mediaTransferEngine.transfer(capture(chunksSlot), any(), any(), any(), any(), any())
            } returns MediaTransferResult.Completed

            repository.runTransfer(
                1,
                DownloadPhase.STREAM,
                "https://example.com/file",
                emptyMap(),
                destinationFile,
                1000L
            )

            expectThat(chunksSlot.captured).hasSize(1)
            expectThat(chunksSlot.captured[0].id).isEqualTo(5L)
        }

    @Test
    fun `requestInterrupt then consumeInterruptReason should return and clear the reason`() {
        repository.requestInterrupt(1, DownloadInterruptReason.PAUSE)

        expectThat(repository.consumeInterruptReason(1)).isEqualTo(DownloadInterruptReason.PAUSE)
        expectThat(repository.consumeInterruptReason(1)).isNull()
    }

    @Test
    fun `getOldestQueuedItem should delegate to the dao filtered by QUEUED state`() =
        runTest {
            val queuedItem = DownloadItem(id = 3, mediaId = "m1", mediaTitle = "Movie", mediaType = MediaType.MOVIE)
            coEvery { downloadItemDao.getOldestByState(DownloadItemState.QUEUED) } returns queuedItem

            val result = repository.getOldestQueuedItem()

            expectThat(result).isEqualTo(queuedItem)
        }

    @Test
    fun `getBatch should delegate to the dao with mediaId and seasonNumber`() =
        runTest {
            val batch = listOf(DownloadItem(id = 1, mediaId = "m1", mediaTitle = "Show", mediaType = MediaType.SHOW))
            coEvery { downloadItemDao.getBatch("m1", 1) } returns batch

            val result = repository.getBatch("m1", 1)

            expectThat(result).isEqualTo(batch)
        }

    @Test
    fun `updateState should delegate to the dao with the given state and phase`() =
        runTest {
            repository.updateState(1, DownloadItemState.PAUSED, DownloadPhase.STREAM)

            coVerify { downloadItemDao.updateState(1, DownloadItemState.PAUSED, DownloadPhase.STREAM, any()) }
        }

    @Test
    fun `markError should delegate to the dao`() =
        runTest {
            repository.markError(1, "boom")

            coVerify { downloadItemDao.updateError(1, "boom", any()) }
        }

    @Test
    fun `resetChunks should delete all chunks for the item`() =
        runTest {
            repository.resetChunks(1)

            coVerify { downloadChunkDao.deleteChunksForItem(1) }
        }

    @Test
    fun `resetChunks should also zero the stream progress columns`() =
        runTest {
            repository.resetChunks(1)

            coVerify { downloadItemDao.updateStreamProgress(1, 0, 0, any()) }
        }

    @Test
    fun `delete should remove the download item`() =
        runTest {
            repository.delete(1)

            coVerify { downloadItemDao.delete(1) }
        }

    @Test
    fun `queue should insert the item and return its generated id`() =
        runTest {
            val item = DownloadItem(mediaId = "m1", mediaTitle = "Movie", mediaType = MediaType.MOVIE)
            coEvery { downloadItemDao.insert(item) } returns 42L

            val id = repository.queue(item)

            expectThat(id).isEqualTo(42L)
        }

    @Test
    fun `runTransfer progress callback should write aggregated bytes to the item on completion status`() =
        runTest {
            val chunk = DownloadChunk(id = 1, downloadItemId = 1, chunkIndex = 0, rangeStart = 0, rangeEnd = 999)
            coEvery { downloadChunkDao.getChunksForItem(1) } returns listOf(chunk)
            coEvery { mediaTransferEngine.transfer(any(), any(), any(), any(), any(), any()) } coAnswers {
                val onProgress = arg<suspend (Long, Long, DownloadChunkStatus) -> Unit>(5)
                onProgress(1, 1000, DownloadChunkStatus.COMPLETED)
                MediaTransferResult.Completed
            }

            repository.runTransfer(
                1,
                DownloadPhase.STREAM,
                "https://example.com/file",
                emptyMap(),
                destinationFile,
                1000L
            )

            coVerify { downloadItemDao.updateStreamProgress(1, any(), 1000L, any()) }
        }

    @Test
    fun `advanceStreamCandidate should return null when there are no fallback candidates`() =
        runTest {
            val item = DownloadItem(id = 1, mediaId = "m1", mediaTitle = "Movie", mediaType = MediaType.MOVIE)
            coEvery { downloadItemDao.get(1) } returns item

            val result = repository.advanceStreamCandidate(1)

            expectThat(result).isNull()
            coVerify(exactly = 0) { downloadItemDao.updateStreamSource(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `advanceStreamCandidate should switch to the next candidate and persist the remaining ones`() =
        runTest {
            val first = DownloadStreamCandidate(url = "https://example.com/a.mp4", isHls = true)
            val second = DownloadStreamCandidate(url = "https://example.com/b.mp4", headers = mapOf("k" to "v"))
            val item = DownloadItem(
                id = 1,
                mediaId = "m1",
                mediaTitle = "Movie",
                mediaType = MediaType.MOVIE,
                streamFallbackCandidates = listOf(first, second),
            )
            coEvery { downloadItemDao.get(1) } returns item

            val result = repository.advanceStreamCandidate(1)

            expectThat(result).isEqualTo(first)
            coVerify {
                downloadItemDao.updateStreamSource(1, first.url, first.headers, listOf(second), true, any())
            }
        }

    @Test
    fun `runHlsTransfer should delegate to the hls transfer engine and clear any pending interrupt`() =
        runTest {
            val segments =
                listOf(
                    HlsSegmentInfo(
                        url = "https://example.com/0.ts",
                        byteRangeOffset = 0,
                        byteRangeLength = -1,
                        encryptionKeyUri = null,
                        encryptionIv = null
                    )
                )
            repository.requestInterrupt(1, DownloadInterruptReason.PAUSE)
            coEvery {
                hlsTransferEngine.transfer(segments, 2, emptyMap(), destinationFile, any(), any())
            } returns MediaTransferResult.Completed

            val result = repository.runHlsTransfer(1, segments, 2, emptyMap(), destinationFile)

            expectThat(result).isEqualTo(MediaTransferResult.Completed)
            expectThat(repository.consumeInterruptReason(1)).isNull()
        }

    @Test
    fun `runHlsTransfer progress callback should write segment counts to the item`() =
        runTest {
            val segments =
                listOf(
                    HlsSegmentInfo(
                        url = "https://example.com/0.ts",
                        byteRangeOffset = 0,
                        byteRangeLength = -1,
                        encryptionKeyUri = null,
                        encryptionIv = null
                    )
                )
            coEvery {
                hlsTransferEngine.transfer(segments, 0, emptyMap(), destinationFile, any(), any())
            } coAnswers {
                val onSegmentWritten = arg<suspend (Int, Int) -> Unit>(5)
                onSegmentWritten(1, 4)
                MediaTransferResult.Completed
            }

            repository.runHlsTransfer(1, segments, 0, emptyMap(), destinationFile)

            coVerify { downloadItemDao.updateStreamProgress(1, 1L, 4L, any()) }
        }
}
