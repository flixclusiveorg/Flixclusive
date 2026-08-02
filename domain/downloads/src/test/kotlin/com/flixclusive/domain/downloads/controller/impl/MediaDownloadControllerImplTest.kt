package com.flixclusive.domain.downloads.controller.impl

import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.data.downloads.model.DownloadInterruptReason
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.flixclusive.domain.downloads.controller.MediaDownloadServiceController
import com.flixclusive.domain.downloads.usecase.GetDownloadDirectoryUseCase
import com.flixclusive.model.media.common.MediaType
import com.hippo.unifile.UniFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException

class MediaDownloadControllerImplTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mediaDownloadRepository: MediaDownloadRepository
    private lateinit var downloadDirectoryRepository: DownloadDirectoryRepository
    private lateinit var getDownloadDirectoryUseCase: GetDownloadDirectoryUseCase
    private lateinit var mediaDownloadServiceController: MediaDownloadServiceController
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var controller: MediaDownloadControllerImpl

    private val directory = mockk<UniFile>(relaxed = true)
    private val subtitlesDirectory = mockk<UniFile>(relaxed = true)
    private val streamFile = mockk<UniFile>()
    private val subtitleFile = mockk<UniFile>()

    private fun testItem(
        id: Long = 1,
        state: DownloadItemState = DownloadItemState.QUEUED,
        phase: DownloadPhase? = null,
        subtitleUrl: String? = null,
    ) = DownloadItem(
        id = id,
        mediaId = "media-1",
        mediaTitle = "Test Movie",
        mediaType = MediaType.MOVIE,
        state = state,
        phase = phase,
        streamUrl = "https://example.com/stream.mp4",
        subtitleUrl = subtitleUrl,
    )

    private fun setConcurrencyLimit(limit: Int) {
        every {
            dataStoreManager.getUserPrefsAsFlow(UserPreferences.DATA_PREFS_KEY, DataPreferences::class)
        } returns flowOf(DataPreferences(downloadConcurrencyLimit = limit))
    }

    @Before
    fun setup() {
        mediaDownloadRepository = mockk(relaxed = true)
        downloadDirectoryRepository = mockk()
        getDownloadDirectoryUseCase = mockk()
        mediaDownloadServiceController = mockk(relaxed = true)
        dataStoreManager = mockk()
        setConcurrencyLimit(3)
        coEvery { mediaDownloadRepository.getOldestQueuedItem() } returns null

        every { downloadDirectoryRepository.getOrCreateFile(directory, any()) } returns streamFile
        every { downloadDirectoryRepository.getOrCreateSubtitlesDirectory(directory) } returns subtitlesDirectory
        every { downloadDirectoryRepository.getOrCreateFile(subtitlesDirectory, any()) } returns subtitleFile

        controller = MediaDownloadControllerImpl(
            mediaDownloadRepository = mediaDownloadRepository,
            downloadDirectoryRepository = downloadDirectoryRepository,
            getDownloadDirectoryUseCase = getDownloadDirectoryUseCase,
            mediaDownloadServiceController = mediaDownloadServiceController,
            dataStoreManager = dataStoreManager,
            appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
        )
    }

    @Test
    fun `start should mark item FAILED when the download directory cannot be resolved`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns null

            controller.start(1)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.FAILED, null) }
            coVerify { mediaDownloadRepository.markError(1, any()) }
        }

    @Test
    fun `start should complete directly when there is no subtitle to fetch`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns testItem(subtitleUrl = null)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(1, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed

            controller.start(1)
            advanceUntilIdle()

            coVerify {
                mediaDownloadRepository.updateState(
                    1,
                    DownloadItemState.DOWNLOADING_STREAM,
                    DownloadPhase.STREAM
                )
            }
            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.STREAM_COMPLETE, null) }
            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.COMPLETED, null) }
            coVerify(
                exactly = 0
            ) { mediaDownloadRepository.updateState(1, DownloadItemState.FETCHING_SUBTITLES, any()) }
        }

    @Test
    fun `start should ensure the media download service is running before transferring`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns testItem(subtitleUrl = null)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(1, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed

            controller.start(1)
            advanceUntilIdle()

            verify { mediaDownloadServiceController.ensureRunning() }
        }

    @Test
    fun `start should fetch subtitles after the stream completes when a subtitle url is set`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns
                testItem(subtitleUrl = "https://example.com/subs.srt")
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(1, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed
            coEvery {
                mediaDownloadRepository.runTransfer(1, DownloadPhase.SUBTITLES, any(), any(), subtitleFile, any())
            } returns MediaTransferResult.Completed

            controller.start(1)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.STREAM_COMPLETE, null) }
            coVerify {
                mediaDownloadRepository.updateState(
                    1,
                    DownloadItemState.FETCHING_SUBTITLES,
                    DownloadPhase.SUBTITLES
                )
            }
            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.COMPLETED, null) }
            coVerify { mediaDownloadRepository.resetChunks(1) }
        }

    @Test
    fun `start should complete with a subtitle error instead of failing the whole item when subtitle fetch fails`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns
                testItem(subtitleUrl = "https://example.com/subs.srt")
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(1, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed
            coEvery {
                mediaDownloadRepository.runTransfer(1, DownloadPhase.SUBTITLES, any(), any(), subtitleFile, any())
            } returns MediaTransferResult.Failed(IOException("subtitle host down"))

            controller.start(1)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.markSubtitleError(1, any()) }
            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.COMPLETED, null) }
            coVerify(exactly = 0) { mediaDownloadRepository.updateState(1, DownloadItemState.FAILED, any()) }
        }

    @Test
    fun `start should mark item FAILED when the stream transfer fails`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(1, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Failed(IOException("boom"))

            controller.start(1)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.markError(1, "boom") }
            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.FAILED, null) }
        }

    @Test
    fun `start should mark item PAUSED when interrupted by a pause request`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(1, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Cancelled
            coEvery { mediaDownloadRepository.consumeInterruptReason(1) } returns DownloadInterruptReason.PAUSE

            controller.start(1)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.PAUSED, DownloadPhase.STREAM) }
            coVerify(exactly = 0) { directory.delete() }
        }

    @Test
    fun `start should mark item STOPPED and delete the directory when interrupted by a stop request`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(1, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Cancelled
            coEvery { mediaDownloadRepository.consumeInterruptReason(1) } returns DownloadInterruptReason.STOP

            controller.start(1)
            advanceUntilIdle()

            coVerify { directory.delete() }
            coVerify { mediaDownloadRepository.resetChunks(1) }
            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.STOPPED, null) }
        }

    @Test
    fun `start should resume directly into the subtitle phase when the item was paused mid-subtitle`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns
                testItem(
                    state = DownloadItemState.PAUSED,
                    phase = DownloadPhase.SUBTITLES,
                    subtitleUrl = "https://example.com/subs.srt"
                )
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(1, DownloadPhase.SUBTITLES, any(), any(), subtitleFile, any())
            } returns MediaTransferResult.Completed

            controller.start(1)
            advanceUntilIdle()

            coVerify(
                exactly = 0
            ) { mediaDownloadRepository.updateState(1, DownloadItemState.DOWNLOADING_STREAM, any()) }
            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.COMPLETED, null) }
        }

    @Test
    fun `retry should reset chunks and requeue before restarting the download`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns testItem(state = DownloadItemState.FAILED)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(1, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed

            controller.retry(1)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.resetChunks(1) }
            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.QUEUED, null) }
            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.COMPLETED, null) }
        }

    @Test
    fun `delete should remove the directory and the persisted item`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory

            controller.delete(1)
            advanceUntilIdle()

            coVerify { directory.delete() }
            coVerify { mediaDownloadRepository.delete(1) }
        }

    @Test
    fun `pause on a queued item should transition it directly to PAUSED without an interrupt flag`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns testItem(state = DownloadItemState.QUEUED)

            controller.pause(1)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.PAUSED, null) }
            coVerify(exactly = 0) { mediaDownloadRepository.requestInterrupt(any(), any()) }
        }

    @Test
    fun `stop on a queued item should clean up and mark STOPPED directly without an interrupt flag`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns testItem(state = DownloadItemState.QUEUED)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory

            controller.stop(1)
            advanceUntilIdle()

            coVerify { directory.delete() }
            coVerify { mediaDownloadRepository.resetChunks(1) }
            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.STOPPED, null) }
            coVerify(exactly = 0) { mediaDownloadRepository.requestInterrupt(any(), any()) }
        }

    @Test
    fun `stop on a paused item should clean up and mark STOPPED directly`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns
                testItem(state = DownloadItemState.PAUSED, phase = DownloadPhase.STREAM)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory

            controller.stop(1)
            advanceUntilIdle()

            coVerify { directory.delete() }
            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.STOPPED, null) }
        }

    @Test
    fun `pause on an actively downloading item should request an interrupt instead of transitioning state directly`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(1) } returns
                testItem(state = DownloadItemState.DOWNLOADING_STREAM)

            controller.pause(1)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.requestInterrupt(1, DownloadInterruptReason.PAUSE) }
            coVerify(exactly = 0) { mediaDownloadRepository.updateState(1, DownloadItemState.PAUSED, any()) }
        }

    @Test
    fun `pauseBatch should pause every item returned for that media and season`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getBatch("media-1", 1) } returns
                listOf(
                    testItem(id = 1, state = DownloadItemState.QUEUED),
                    testItem(id = 2, state = DownloadItemState.QUEUED)
                )
            coEvery { mediaDownloadRepository.getItem(1) } returns testItem(id = 1)
            coEvery { mediaDownloadRepository.getItem(2) } returns testItem(id = 2)

            controller.pauseBatch("media-1", 1)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.PAUSED, null) }
            coVerify { mediaDownloadRepository.updateState(2, DownloadItemState.PAUSED, null) }
        }

    @Test
    fun `stopBatch should stop every item returned for that media and season`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getBatch("media-1", 1) } returns
                listOf(
                    testItem(id = 1, state = DownloadItemState.QUEUED),
                    testItem(id = 2, state = DownloadItemState.QUEUED)
                )
            coEvery { mediaDownloadRepository.getItem(1) } returns testItem(id = 1)
            coEvery { mediaDownloadRepository.getItem(2) } returns testItem(id = 2)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory

            controller.stopBatch("media-1", 1)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState(1, DownloadItemState.STOPPED, null) }
            coVerify { mediaDownloadRepository.updateState(2, DownloadItemState.STOPPED, null) }
        }

    @Test
    fun `a second item should not start until the first releases its concurrency slot`() =
        runTest(testDispatcher) {
            setConcurrencyLimit(1)

            val item1Gate = CompletableDeferred<Unit>()

            coEvery { mediaDownloadRepository.getItem(1) } returns testItem(id = 1)
            coEvery { mediaDownloadRepository.getItem(2) } returns testItem(id = 2)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(1, DownloadPhase.STREAM, any(), any(), any(), any())
            } coAnswers {
                item1Gate.await()
                MediaTransferResult.Completed
            }
            coEvery {
                mediaDownloadRepository.runTransfer(2, DownloadPhase.STREAM, any(), any(), any(), any())
            } returns MediaTransferResult.Completed
            coEvery { mediaDownloadRepository.getOldestQueuedItem() } returns testItem(id = 2) andThen null

            controller.start(1)
            controller.start(2)
            advanceUntilIdle()

            // Item 1 is still mid-transfer (blocked on item1Gate), so item 2 must not have gotten a slot.
            coVerify(exactly = 1) {
                mediaDownloadRepository.updateState(1, DownloadItemState.DOWNLOADING_STREAM, DownloadPhase.STREAM)
            }
            coVerify(
                exactly = 0
            ) { mediaDownloadRepository.updateState(2, DownloadItemState.DOWNLOADING_STREAM, any()) }

            item1Gate.complete(Unit)
            advanceUntilIdle()

            // Once item 1 finishes and frees its slot, dispatchNext() should pick item 2 up.
            coVerify(exactly = 1) {
                mediaDownloadRepository.updateState(2, DownloadItemState.DOWNLOADING_STREAM, DownloadPhase.STREAM)
            }
        }
}
