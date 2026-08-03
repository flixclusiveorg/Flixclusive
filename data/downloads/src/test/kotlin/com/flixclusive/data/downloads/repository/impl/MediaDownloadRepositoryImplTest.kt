package com.flixclusive.data.downloads.repository.impl

import com.flixclusive.core.database.dao.downloads.DownloadChunkDao
import com.flixclusive.core.database.dao.downloads.DownloadItemDao
import com.flixclusive.core.database.entity.downloads.DownloadChunk
import com.flixclusive.core.database.entity.downloads.DownloadChunkStatus
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
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
    private val itemId = "item-1"

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
            coEvery { downloadChunkDao.getChunksForItem(itemId) } returnsMany listOf(emptyList(), emptyList())
            coEvery {
                mediaTransferEngine.transfer(any(), any(), any(), any(), any(), any())
            } returns MediaTransferResult.Completed

            repository.runTransfer(
                itemId,
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
                listOf(DownloadChunk(id = 1, downloadItemId = itemId, chunkIndex = 0, rangeStart = 0, rangeEnd = 999))
            coEvery { downloadChunkDao.getChunksForItem(itemId) } returns existing
            coEvery {
                mediaTransferEngine.transfer(any(), any(), any(), any(), any(), any())
            } returns MediaTransferResult.Completed

            repository.runTransfer(
                itemId,
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
                listOf(DownloadChunk(id = 5, downloadItemId = itemId, chunkIndex = 0, rangeStart = 0, rangeEnd = 999))
            coEvery { downloadChunkDao.getChunksForItem(itemId) } returns existing
            val chunksSlot = slot<List<DownloadChunk>>()
            coEvery {
                mediaTransferEngine.transfer(capture(chunksSlot), any(), any(), any(), any(), any())
            } returns MediaTransferResult.Completed

            repository.runTransfer(
                itemId,
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
        repository.requestInterrupt(itemId, DownloadInterruptReason.PAUSE)

        expectThat(repository.consumeInterruptReason(itemId)).isEqualTo(DownloadInterruptReason.PAUSE)
        expectThat(repository.consumeInterruptReason(itemId)).isNull()
    }

    @Test
    fun `getOldestQueuedItem should delegate to the dao filtered by QUEUED state`() =
        runTest {
            val queuedItem = DownloadItem(
                id = "item-3",
                ownerId = "owner-1",
                mediaId = "m1",
                mediaTitle = "Movie",
                mediaType = MediaType.MOVIE,
            )
            coEvery { downloadItemDao.getOldestByState(DownloadItemState.QUEUED) } returns queuedItem

            val result = repository.getOldestQueuedItem()

            expectThat(result).isEqualTo(queuedItem)
        }

    @Test
    fun `getBatch should delegate to the dao with mediaId and seasonNumber`() =
        runTest {
            val batch = listOf(
                DownloadItem(
                    id = itemId,
                    ownerId = "owner-1",
                    mediaId = "m1",
                    mediaTitle = "Show",
                    mediaType = MediaType.SHOW,
                )
            )
            coEvery { downloadItemDao.getBatch("m1", 1) } returns batch

            val result = repository.getBatch("m1", 1)

            expectThat(result).isEqualTo(batch)
        }

    @Test
    fun `updateState should delegate to the dao with the given state and phase`() =
        runTest {
            repository.updateState(itemId, DownloadItemState.PAUSED, DownloadPhase.STREAM)

            coVerify { downloadItemDao.updateState(itemId, DownloadItemState.PAUSED, DownloadPhase.STREAM, any()) }
        }

    @Test
    fun `markError should delegate to the dao`() =
        runTest {
            repository.markError(itemId, "boom")

            coVerify { downloadItemDao.updateError(itemId, "boom", any()) }
        }

    @Test
    fun `resetChunks should delete all chunks for the item`() =
        runTest {
            repository.resetChunks(itemId)

            coVerify { downloadChunkDao.deleteChunksForItem(itemId) }
        }

    @Test
    fun `resetChunks should also zero the stream progress columns`() =
        runTest {
            repository.resetChunks(itemId)

            coVerify { downloadItemDao.updateStreamProgress(itemId, 0, 0, any()) }
        }

    @Test
    fun `delete should remove the download item`() =
        runTest {
            repository.delete(itemId)

            coVerify { downloadItemDao.delete(itemId) }
        }

    @Test
    fun `queue should insert the item`() =
        runTest {
            val item = DownloadItem(
                ownerId = "owner-1",
                mediaId = "m1",
                mediaTitle = "Movie",
                mediaType = MediaType.MOVIE,
            )

            repository.queue(item)

            coVerify { downloadItemDao.insert(item) }
        }

    @Test
    fun `updateSource should always write sourceUrl and isHlsStream together`() =
        runTest {
            repository.updateSource(itemId, "https://example.com/fallback.m3u8", isHls = true)

            coVerify {
                downloadItemDao.updateSource(itemId, "https://example.com/fallback.m3u8", true, any())
            }
        }

    @Test
    fun `updateStreamFilePath should delegate to the dao`() =
        runTest {
            repository.updateStreamFilePath(itemId, "content://tree/video.mp4")

            coVerify { downloadItemDao.updateStreamFilePath(itemId, "content://tree/video.mp4", any()) }
        }

    @Test
    fun `setTotalSubtitlesCount should delegate to the dao`() =
        runTest {
            repository.setTotalSubtitlesCount(itemId, 3)

            coVerify { downloadItemDao.setTotalSubtitlesCount(itemId, 3, any()) }
        }

    @Test
    fun `incrementDownloadedSubtitlesCount should delegate to the dao`() =
        runTest {
            repository.incrementDownloadedSubtitlesCount(itemId)

            coVerify { downloadItemDao.incrementDownloadedSubtitlesCount(itemId, any()) }
        }

    @Test
    fun `runTransfer progress callback should write aggregated bytes for the STREAM phase on completion status`() =
        runTest {
            val chunk = DownloadChunk(id = 1, downloadItemId = itemId, chunkIndex = 0, rangeStart = 0, rangeEnd = 999)
            coEvery { downloadChunkDao.getChunksForItem(itemId) } returns listOf(chunk)
            coEvery { mediaTransferEngine.transfer(any(), any(), any(), any(), any(), any()) } coAnswers {
                val onProgress = arg<suspend (Long, Long, DownloadChunkStatus) -> Unit>(5)
                onProgress(1, 1000, DownloadChunkStatus.COMPLETED)
                MediaTransferResult.Completed
            }

            repository.runTransfer(
                itemId,
                DownloadPhase.STREAM,
                "https://example.com/file",
                emptyMap(),
                destinationFile,
                1000L
            )

            coVerify { downloadItemDao.updateStreamProgress(itemId, any(), 1000L, any()) }
        }

    @Test
    fun `runTransfer progress callback should not write item byte progress for the SUBTITLES phase`() =
        runTest {
            val chunk = DownloadChunk(id = 1, downloadItemId = itemId, chunkIndex = 0, rangeStart = 0, rangeEnd = 999)
            coEvery { downloadChunkDao.getChunksForItem(itemId) } returns listOf(chunk)
            coEvery { mediaTransferEngine.transfer(any(), any(), any(), any(), any(), any()) } coAnswers {
                val onProgress = arg<suspend (Long, Long, DownloadChunkStatus) -> Unit>(5)
                onProgress(1, 1000, DownloadChunkStatus.COMPLETED)
                MediaTransferResult.Completed
            }

            repository.runTransfer(
                itemId,
                DownloadPhase.SUBTITLES,
                "https://example.com/subs.srt",
                emptyMap(),
                destinationFile,
                null
            )

            coVerify(exactly = 0) { downloadItemDao.updateStreamProgress(itemId, any(), any(), any()) }
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
            repository.requestInterrupt(itemId, DownloadInterruptReason.PAUSE)
            coEvery {
                hlsTransferEngine.transfer(segments, 2, emptyMap(), destinationFile, any(), any())
            } returns MediaTransferResult.Completed

            val result = repository.runHlsTransfer(itemId, segments, 2, emptyMap(), destinationFile)

            expectThat(result).isEqualTo(MediaTransferResult.Completed)
            expectThat(repository.consumeInterruptReason(itemId)).isNull()
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

            repository.runHlsTransfer(itemId, segments, 0, emptyMap(), destinationFile)

            coVerify { downloadItemDao.updateStreamProgress(itemId, 1L, 4L, any()) }
        }
}
