package com.flixclusive.data.downloads.repository.impl

import app.cash.turbine.test
import com.flixclusive.core.network.download.CoroutineDownloader
import com.flixclusive.core.network.download.DownloadProgress
import com.flixclusive.data.downloads.model.DownloadStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.io.File
import java.io.IOException

class DownloadRepositoryImplTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var coroutineDownloader: CoroutineDownloader
    private lateinit var downloadRepository: DownloadRepositoryImpl

    private val mockFile = mockk<File>()

    @Before
    fun setup() {
        coroutineDownloader = mockk()
        downloadRepository = DownloadRepositoryImpl(coroutineDownloader)
    }

    @Test
    fun `when getting download state for new id then returns initial state`() =
        runTest(testDispatcher) {
            val downloadId = "test_download_1"

            val stateFlow = downloadRepository.getDownloadState(downloadId)

            stateFlow.test {
                val initialState = awaitItem()
                expectThat(initialState.downloadId).isEqualTo(downloadId)
                expectThat(initialState.status).isEqualTo(DownloadStatus.IDLE)
                expectThat(initialState.progress).isEqualTo(0)
                expectThat(initialState.bytesDownloaded).isEqualTo(0L)
                expectThat(initialState.totalBytes).isEqualTo(0L)
                expectThat(initialState.error).isNull()
                cancelAndIgnoreRemainingEvents()
            }

            advanceUntilIdle()
        }

    @Test
    fun `when getting same download state multiple times then returns same flow`() =
        runTest(testDispatcher) {
            val downloadId = "test_download_1"

            val stateFlow1 = downloadRepository.getDownloadState(downloadId)
            val stateFlow2 = downloadRepository.getDownloadState(downloadId)

            expectThat(stateFlow1.value).isEqualTo(stateFlow2.value)

            advanceUntilIdle()
        }

    @Test
    fun `when download succeeds with progress then updates state correctly`() =
        runTest(testDispatcher) {
            val downloadId = "test_download_1"
            val url = "https://example.com/file.apk"
            val progressSteps = listOf(
                DownloadProgress(bytesDownloaded = 250L, totalBytes = 1000L, progress = 25, isComplete = false),
                DownloadProgress(bytesDownloaded = 500L, totalBytes = 1000L, progress = 50, isComplete = false),
                DownloadProgress(bytesDownloaded = 750L, totalBytes = 1000L, progress = 75, isComplete = false),
                DownloadProgress(bytesDownloaded = 1000L, totalBytes = 1000L, progress = 100, isComplete = true),
            )

            every { coroutineDownloader.download(url, mockFile) } returns flow {
                progressSteps.forEach { emit(it) }
            }

            val stateFlow = downloadRepository.getDownloadState(downloadId)

            stateFlow.test {
                val initialState = awaitItem()
                expectThat(initialState.status).isEqualTo(DownloadStatus.IDLE)

                downloadRepository.executeDownload(downloadId, url, mockFile)

                val downloadingState = awaitItem()
                expectThat(downloadingState.status).isEqualTo(DownloadStatus.DOWNLOADING)

                val progress25State = awaitItem()
                expectThat(progress25State.status).isEqualTo(DownloadStatus.DOWNLOADING)
                expectThat(progress25State.progress).isEqualTo(25)
                expectThat(progress25State.bytesDownloaded).isEqualTo(250L)
                expectThat(progress25State.totalBytes).isEqualTo(1000L)

                val progress50State = awaitItem()
                expectThat(progress50State.status).isEqualTo(DownloadStatus.DOWNLOADING)
                expectThat(progress50State.progress).isEqualTo(50)
                expectThat(progress50State.bytesDownloaded).isEqualTo(500L)

                val progress75State = awaitItem()
                expectThat(progress75State.status).isEqualTo(DownloadStatus.DOWNLOADING)
                expectThat(progress75State.progress).isEqualTo(75)
                expectThat(progress75State.bytesDownloaded).isEqualTo(750L)

                val completedState = awaitItem()
                expectThat(completedState.status).isEqualTo(DownloadStatus.COMPLETED)
                expectThat(completedState.progress).isEqualTo(100)
                expectThat(completedState.bytesDownloaded).isEqualTo(1000L)
                expectThat(completedState.totalBytes).isEqualTo(1000L)

                cancelAndIgnoreRemainingEvents()
            }

            advanceUntilIdle()
        }

    @Test
    fun `when download fails then updates state with error`() =
        runTest(testDispatcher) {
            val downloadId = "test_download_1"
            val url = "https://example.com/file.apk"
            val errorMessage = "Network error"
            val exception = IOException(errorMessage)

            every { coroutineDownloader.download(url, mockFile) } throws exception

            val stateFlow = downloadRepository.getDownloadState(downloadId)

            stateFlow.test {
                val initialState = awaitItem()
                expectThat(initialState.status).isEqualTo(DownloadStatus.IDLE)

                downloadRepository.executeDownload(downloadId, url, mockFile)

                val downloadingState = awaitItem()
                expectThat(downloadingState.status).isEqualTo(DownloadStatus.DOWNLOADING)

                val failedState = awaitItem()
                expectThat(failedState.status).isEqualTo(DownloadStatus.FAILED)
                expectThat(failedState.error?.toString()).isEqualTo(errorMessage)

                cancelAndIgnoreRemainingEvents()
            }

            advanceUntilIdle()
        }

    @Test
    fun `when download is cancelled then updates state to cancelled`() =
        runTest(testDispatcher) {
            val downloadId = "test_download_1"

            val stateFlow = downloadRepository.getDownloadState(downloadId)

            stateFlow.test {
                val initialState = awaitItem()
                expectThat(initialState.status).isEqualTo(DownloadStatus.IDLE)

                downloadRepository.cancelDownload(downloadId)

                val cancelledState = awaitItem()
                expectThat(cancelledState.status).isEqualTo(DownloadStatus.CANCELLED)

                cancelAndIgnoreRemainingEvents()
            }

            advanceUntilIdle()
        }

    @Test
    fun `when multiple downloads run concurrently then each maintains separate state`() =
        runTest(testDispatcher) {
            val downloadId1 = "test_download_1"
            val downloadId2 = "test_download_2"
            val url1 = "https://example.com/file1.apk"
            val url2 = "https://example.com/file2.apk"

            val progress1 = listOf(
                DownloadProgress(bytesDownloaded = 500L, totalBytes = 1000L, progress = 50, isComplete = false),
                DownloadProgress(bytesDownloaded = 1000L, totalBytes = 1000L, progress = 100, isComplete = true),
            )

            val progress2 = listOf(
                DownloadProgress(bytesDownloaded = 250L, totalBytes = 500L, progress = 50, isComplete = false),
                DownloadProgress(bytesDownloaded = 500L, totalBytes = 500L, progress = 100, isComplete = true),
            )

            every { coroutineDownloader.download(url1, any()) } returns flow {
                progress1.forEach { emit(it) }
            }

            every { coroutineDownloader.download(url2, any()) } returns flow {
                progress2.forEach { emit(it) }
            }

            val stateFlow1 = downloadRepository.getDownloadState(downloadId1)
            val stateFlow2 = downloadRepository.getDownloadState(downloadId2)

            stateFlow1.test {
                val initialState1 = awaitItem()
                expectThat(initialState1.downloadId).isEqualTo(downloadId1)
                expectThat(initialState1.status).isEqualTo(DownloadStatus.IDLE)

                downloadRepository.executeDownload(downloadId1, url1, mockFile)

                skipItems(2) // Skip downloading and progress states
                val completedState1 = awaitItem()
                expectThat(completedState1.status).isEqualTo(DownloadStatus.COMPLETED)
                expectThat(completedState1.totalBytes).isEqualTo(1000L)

                cancelAndIgnoreRemainingEvents()
            }

            stateFlow2.test {
                val initialState2 = awaitItem()
                expectThat(initialState2.downloadId).isEqualTo(downloadId2)
                expectThat(initialState2.status).isEqualTo(DownloadStatus.IDLE)

                downloadRepository.executeDownload(downloadId2, url2, mockFile)

                skipItems(2) // Skip downloading and progress states
                val completedState2 = awaitItem()
                expectThat(completedState2.status).isEqualTo(DownloadStatus.COMPLETED)
                expectThat(completedState2.totalBytes).isEqualTo(500L)

                cancelAndIgnoreRemainingEvents()
            }

            advanceUntilIdle()
        }

    @Test
    fun `when download progress has zero total bytes then handles gracefully`() =
        runTest(testDispatcher) {
            val downloadId = "test_download_1"
            val url = "https://example.com/file.apk"
            val progressWithZeroTotal = DownloadProgress(
                bytesDownloaded = 100L,
                totalBytes = 0L,
                progress = 0,
                isComplete = true,
            )

            every { coroutineDownloader.download(url, mockFile) } returns flowOf(progressWithZeroTotal)

            val stateFlow = downloadRepository.getDownloadState(downloadId)

            stateFlow.test {
                val initialState = awaitItem()
                expectThat(initialState.status).isEqualTo(DownloadStatus.IDLE)

                downloadRepository.executeDownload(downloadId, url, mockFile)

                val downloadingState = awaitItem()
                expectThat(downloadingState.status).isEqualTo(DownloadStatus.DOWNLOADING)

                val completedState = awaitItem()
                expectThat(completedState.status).isEqualTo(DownloadStatus.COMPLETED)
                expectThat(completedState.progress).isEqualTo(0)
                expectThat(completedState.bytesDownloaded).isEqualTo(100L)
                expectThat(completedState.totalBytes).isEqualTo(0L)

                cancelAndIgnoreRemainingEvents()
            }

            advanceUntilIdle()
        }
}
