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
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import com.flixclusive.model.media.common.MediaType
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
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
    fun `resetChunks should also zero the stream progress and speed columns`() =
        runTest {
            repository.resetChunks(itemId)

            coVerify { downloadItemDao.updateStreamProgress(itemId, 0, 0, 0, any()) }
        }

    @Test
    fun `deleteChunks should delete chunks without touching stream progress`() =
        runTest {
            repository.deleteChunks(itemId)

            coVerify { downloadChunkDao.deleteChunksForItem(itemId) }
            coVerify(exactly = 0) { downloadItemDao.updateStreamProgress(any(), any(), any(), any(), any()) }
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
            repository.updateSource(itemId, "https://example.com/fallback.m3u8", isHls = true, totalBytes = 500L)

            coVerify {
                downloadItemDao.updateSource(itemId, "https://example.com/fallback.m3u8", true, 500L, any())
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

            coVerify { downloadItemDao.updateStreamProgress(itemId, any(), 1000L, any(), any()) }
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

            coVerify(exactly = 0) { downloadItemDao.updateStreamProgress(itemId, any(), any(), any(), any()) }
        }

    @Test
    fun `runTransfer progress callback should still write a download rate for the SUBTITLES phase`() =
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

            coVerify { downloadItemDao.updateDownloadRate(itemId, any(), any()) }
        }

    @Test
    fun `runTransfer progress callback should not write a download rate for the STREAM phase`() =
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

            coVerify(exactly = 0) { downloadItemDao.updateDownloadRate(any(), any(), any()) }
        }

    @Test
    fun `runHlsTransfer should delegate to the hls transfer engine`() =
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
                hlsTransferEngine.transfer(segments, 2, emptyMap(), destinationFile, any(), any())
            } returns MediaTransferResult.Completed

            val result = repository.runHlsTransfer(itemId, segments, 2, emptyMap(), destinationFile)

            expectThat(result).isEqualTo(MediaTransferResult.Completed)
        }

    @Test
    fun `runHlsTransfer should cancel up front on a pending interrupt, leaving it for the caller`() =
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

            val result = repository.runHlsTransfer(itemId, segments, 2, emptyMap(), destinationFile)

            expectThat(result).isEqualTo(MediaTransferResult.Cancelled)
            coVerify(exactly = 0) { hlsTransferEngine.transfer(any(), any(), any(), any(), any(), any()) }
            // Left set, not wiped: the caller consumes it to decide between PAUSED and STOPPED.
            expectThat(repository.consumeInterruptReason(itemId)).isEqualTo(DownloadInterruptReason.PAUSE)
        }

    @Test
    fun `runTransfer should cancel up front on a pending interrupt, leaving it for the caller`() =
        runTest {
            repository.requestInterrupt(itemId, DownloadInterruptReason.STOP)

            val result = repository.runTransfer(
                itemId,
                DownloadPhase.STREAM,
                "https://example.com/file",
                emptyMap(),
                destinationFile,
                1000L
            )

            expectThat(result).isEqualTo(MediaTransferResult.Cancelled)
            coVerify(exactly = 0) { mediaTransferEngine.transfer(any(), any(), any(), any(), any(), any()) }
            expectThat(repository.consumeInterruptReason(itemId)).isEqualTo(DownloadInterruptReason.STOP)
        }

    @Test
    fun `requeueInterruptedItems should requeue stream and subtitle phases into the phase each resumes at`() =
        runTest {
            coEvery { downloadItemDao.requeueByStates(any(), any(), any(), any(), any()) } returns 1

            val requeued = repository.requeueInterruptedItems(listOf("live-item"))

            expectThat(requeued).isEqualTo(2)
            coVerify {
                downloadItemDao.requeueByStates(
                    listOf(DownloadItemState.DOWNLOADING_STREAM),
                    DownloadItemState.QUEUED,
                    DownloadPhase.STREAM,
                    listOf("live-item"),
                    any(),
                )
            }
            coVerify {
                downloadItemDao.requeueByStates(
                    listOf(DownloadItemState.FETCHING_SUBTITLES, DownloadItemState.STREAM_COMPLETE),
                    DownloadItemState.QUEUED,
                    DownloadPhase.SUBTITLES,
                    listOf("live-item"),
                    any(),
                )
            }
        }

    @Test
    fun `progress rate should hold the last measured speed for three stalled samples then report zero`() =
        runTest {
            val rates = captureStreamRatesFor(
                byteTotals = listOf(0L, 4_000L, 4_000L, 4_000L, 4_000L, 4_000L),
            )

            expectThat(rates).hasSize(6)
            // Nothing to diff the very first sample against.
            expectThat(rates[0]).isEqualTo(0L)
            expectThat(rates[1]).isGreaterThan(0L)
            // Three consecutive stalled samples replay the last measured speed…
            expectThat(rates.subList(2, 5)).isEqualTo(listOf(rates[1], rates[1], rates[1]))
            // …and the fourth gives up on it.
            expectThat(rates[5]).isEqualTo(0L)
        }

    @Test
    fun `progress rate should restart its hold window once bytes start moving again`() =
        runTest {
            val rates = captureStreamRatesFor(
                byteTotals = listOf(0L, 4_000L, 4_000L, 8_000L, 8_000L, 8_000L, 8_000L, 8_000L),
            )

            // Index 3 moves again, so the two stalled samples before it don't count towards the
            // window — indices 4..6 hold at the new speed and only index 7 falls back to zero.
            expectThat(rates[3]).isGreaterThan(0L)
            expectThat(rates.subList(4, 7)).isEqualTo(listOf(rates[3], rates[3], rates[3]))
            expectThat(rates[7]).isEqualTo(0L)
        }

    /**
     * Drives one STREAM transfer whose chunk total walks through [byteTotals], and returns the rate
     * persisted for each. Progress is reported as [DownloadChunkStatus.COMPLETED] so every sample
     * bypasses the one-second write throttle, and each is separated by a real (tiny) sleep because
     * the repository derives its rate from the wall clock.
     */
    private suspend fun captureStreamRatesFor(byteTotals: List<Long>): List<Long> {
        val rates = mutableListOf<Long>()
        coEvery { downloadItemDao.updateStreamProgress(itemId, any(), any(), capture(rates), any()) } just Runs
        coEvery { downloadChunkDao.getChunksForItem(itemId) } returnsMany
            // The first read is runTransfer's own chunk lookup, before any progress is reported.
            (listOf(0L) + byteTotals).map { listOf(chunkWith(bytesDownloaded = it)) }
        coEvery { mediaTransferEngine.transfer(any(), any(), any(), any(), any(), any()) } coAnswers {
            val onProgress = arg<suspend (Long, Long, DownloadChunkStatus) -> Unit>(5)
            repeat(byteTotals.size) {
                Thread.sleep(RATE_SAMPLE_SPACING_MS)
                onProgress(1, 10_000, DownloadChunkStatus.COMPLETED)
            }
            MediaTransferResult.Completed
        }

        repository.runTransfer(
            itemId,
            DownloadPhase.STREAM,
            "https://example.com/file",
            emptyMap(),
            destinationFile,
            10_000L
        )

        return rates
    }

    private fun chunkWith(bytesDownloaded: Long) = DownloadChunk(
        id = 1,
        downloadItemId = itemId,
        chunkIndex = 0,
        rangeStart = 0,
        rangeEnd = 9_999,
        bytesDownloaded = bytesDownloaded,
    )

    private companion object {
        const val RATE_SAMPLE_SPACING_MS = 5L
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

            coVerify { downloadItemDao.updateStreamProgress(itemId, 1L, 4L, any(), any()) }
        }

    @Test
    fun `runHlsTransfer progress callback rate should be zero on the item's first ever sample`() =
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

            // No prior write for itemId exists yet, so there's nothing to diff a rate against.
            repository.runHlsTransfer(itemId, segments, 0, emptyMap(), destinationFile)

            coVerify { downloadItemDao.updateStreamProgress(itemId, 1L, 4L, 0L, any()) }
        }
}
