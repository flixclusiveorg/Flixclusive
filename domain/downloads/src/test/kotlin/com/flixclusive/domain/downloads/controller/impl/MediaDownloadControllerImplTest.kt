package com.flixclusive.domain.downloads.controller.impl

import android.content.Context
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.core.database.entity.provider.CachedSubtitle
import com.flixclusive.core.database.entity.provider.MediaLinksWithData
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.monitor.NetworkMonitor
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.data.downloads.hls.HlsManifestResolver
import com.flixclusive.data.downloads.hls.HlsResolutionResult
import com.flixclusive.data.downloads.hls.HlsSegmentInfo
import com.flixclusive.data.downloads.hls.ResolvedHlsPlaylist
import com.flixclusive.data.downloads.model.DownloadInterruptReason
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.domain.downloads.controller.MediaDownloadServiceController
import com.flixclusive.domain.downloads.usecase.GetDownloadDirectoryUseCase
import com.flixclusive.domain.downloads.usecase.RankedDownloadCandidate
import com.flixclusive.domain.downloads.usecase.ResolveDownloadableStreamUseCase
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.provider.link.Stream
import com.hippo.unifile.UniFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException

class MediaDownloadControllerImplTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mediaDownloadRepository: MediaDownloadRepository
    private lateinit var mediaLinksRepository: MediaLinksRepository
    private lateinit var resolveDownloadableStreamUseCase: ResolveDownloadableStreamUseCase
    private lateinit var downloadDirectoryRepository: DownloadDirectoryRepository
    private lateinit var getDownloadDirectoryUseCase: GetDownloadDirectoryUseCase
    private lateinit var hlsManifestResolver: HlsManifestResolver
    private lateinit var mediaDownloadServiceController: MediaDownloadServiceController
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var controller: MediaDownloadControllerImpl

    private val directory = mockk<UniFile>(relaxed = true)
    private val subtitlesDirectory = mockk<UniFile>(relaxed = true)
    private val streamFile = mockk<UniFile>(relaxed = true)
    private val subtitleFile = mockk<UniFile>(relaxed = true)

    private val ownerId = "owner-1"
    private val mediaId = "media-1"
    private val itemId = "item-1"

    private val media = DBMedia(
        id = mediaId,
        title = "Test Movie",
        providerId = "test-provider",
        adult = false,
        type = MediaType.MOVIE,
        overview = null,
        posterImage = null,
        language = null,
        rating = null,
        backdropImage = null,
        releaseDate = null,
    )

    private fun testItem(
        id: String = itemId,
        state: DownloadItemState = DownloadItemState.QUEUED,
        phase: DownloadPhase? = null,
        sourceUrl: String? = "https://example.com/stream.mp4",
        isHlsStream: Boolean = false,
        streamFilePath: String? = null,
        streamBytesDownloaded: Long = 0,
        downloadedSubtitlesCount: Int = 0,
        totalSubtitlesCount: Int = 0,
    ) = DownloadItem(
        id = id,
        ownerId = ownerId,
        mediaId = mediaId,
        mediaTitle = "Test Movie",
        mediaType = MediaType.MOVIE,
        state = state,
        phase = phase,
        sourceUrl = sourceUrl,
        isHlsStream = isHlsStream,
        streamFilePath = streamFilePath,
        streamBytesDownloaded = streamBytesDownloaded,
        downloadedSubtitlesCount = downloadedSubtitlesCount,
        totalSubtitlesCount = totalSubtitlesCount,
    )

    private fun cachedStream(url: String) = CachedStream(
        url = url,
        label = "test",
        providerId = "test-provider",
        ownerId = ownerId,
        mediaId = mediaId,
    )

    private fun cachedSubtitle(label: String, url: String) = CachedSubtitle(
        url = url,
        label = label,
        providerId = "test-provider",
        ownerId = ownerId,
        mediaId = mediaId,
    )

    private var preferences = DataPreferences()

    private fun stubPreferences() {
        every {
            dataStoreManager.getUserPrefsAsFlow(UserPreferences.DATA_PREFS_KEY, DataPreferences::class)
        } returns flowOf(preferences)
    }

    private fun setConcurrencyLimit(limit: Int) {
        preferences = preferences.copy(downloadConcurrencyLimit = limit)
        stubPreferences()
    }

    private fun setWifiOnly(enabled: Boolean) {
        preferences = preferences.copy(downloadOnWifiOnly = enabled)
        stubPreferences()
    }

    private fun setMetered(metered: Boolean) {
        every { networkMonitor.isMetered } returns flowOf(metered)
    }

    @Before
    fun setup() {
        mediaDownloadRepository = mockk(relaxed = true)
        mediaLinksRepository = mockk(relaxed = true)
        resolveDownloadableStreamUseCase = mockk()
        downloadDirectoryRepository = mockk()
        getDownloadDirectoryUseCase = mockk()
        hlsManifestResolver = mockk()
        mediaDownloadServiceController = mockk(relaxed = true)
        dataStoreManager = mockk()
        networkMonitor = mockk()
        preferences = DataPreferences()
        setConcurrencyLimit(3)
        // Unmetered by default so the Wi-Fi-only gate never interferes with tests that aren't
        // about it; the gating tests set this explicitly.
        setMetered(false)
        coEvery { mediaDownloadRepository.getOldestQueuedItem() } returns null
        // Relaxed mocks default an unstubbed enum-returning call to its first declared constant
        // rather than null, so without this every runSubtitlePhase() per-file interrupt pre-check
        // would see a phantom pending PAUSE. Tests exercising an actual pause/stop override this.
        coEvery { mediaDownloadRepository.consumeInterruptReason(any()) } returns null
        coEvery { mediaLinksRepository.getLinks(any(), any(), any(), any()) } returns emptyList()

        every { downloadDirectoryRepository.getOrCreateFile(directory, any()) } returns streamFile
        every { downloadDirectoryRepository.getOrCreateSubtitlesDirectory(directory) } returns subtitlesDirectory
        every { downloadDirectoryRepository.getOrCreateFile(subtitlesDirectory, any()) } returns subtitleFile
        every { downloadDirectoryRepository.resolveFile(any()) } returns null
        every { streamFile.length() } returns 200_000L

        controller = MediaDownloadControllerImpl(
            context = mockk<Context>(),
            mediaDownloadRepository = mediaDownloadRepository,
            mediaLinksRepository = mediaLinksRepository,
            resolveDownloadableStreamUseCase = resolveDownloadableStreamUseCase,
            downloadDirectoryRepository = downloadDirectoryRepository,
            getDownloadDirectoryUseCase = getDownloadDirectoryUseCase,
            hlsManifestResolver = hlsManifestResolver,
            networkMonitor = networkMonitor,
            mediaDownloadServiceController = mediaDownloadServiceController,
            dataStoreManager = dataStoreManager,
            appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
        )
    }

    @Test
    fun `start should mark item FAILED when the download directory cannot be resolved`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns null

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.FAILED, null) }
            coVerify { mediaDownloadRepository.markError(itemId, any()) }
        }

    @Test
    fun `start should complete directly when there are no valid cached subtitles`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed

            controller.start(itemId)
            advanceUntilIdle()

            coVerify {
                mediaDownloadRepository.updateState(
                    itemId,
                    DownloadItemState.DOWNLOADING_STREAM,
                    DownloadPhase.STREAM
                )
            }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.STREAM_COMPLETE, null) }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null) }
            coVerify(
                exactly = 0
            ) { mediaDownloadRepository.updateState(itemId, DownloadItemState.FETCHING_SUBTITLES, any()) }
        }

    @Test
    fun `start should ensure the media download service is running before transferring`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed

            controller.start(itemId)
            advanceUntilIdle()

            verify { mediaDownloadServiceController.ensureRunning() }
        }

    @Test
    fun `start should persist the destination file path when creating it for the first time`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem(streamFilePath = null)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateStreamFilePath(itemId, any()) }
        }

    @Test
    fun `start should reuse the persisted stream file path on resume instead of re-deriving the file name`() =
        runTest(testDispatcher) {
            val existingFile = mockk<UniFile>(relaxed = true)
            every { existingFile.length() } returns 200_000L
            every { downloadDirectoryRepository.resolveFile("content://tree/existing.mp4") } returns existingFile

            coEvery { mediaDownloadRepository.getItem(itemId) } returns
                testItem(streamFilePath = "content://tree/existing.mp4")
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), existingFile, any())
            } returns MediaTransferResult.Completed

            controller.start(itemId)
            advanceUntilIdle()

            verify(exactly = 0) { downloadDirectoryRepository.getOrCreateFile(directory, any()) }
            coVerify(exactly = 0) { mediaDownloadRepository.updateStreamFilePath(any(), any()) }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null) }
        }

    @Test
    fun `start should resolve a fresh cached link when the item has no source url yet`() =
        runTest(testDispatcher) {
            val resolvedUrl = "https://example.com/resolved.mp4"

            coEvery { mediaDownloadRepository.getItem(itemId) } returnsMany
                listOf(testItem(sourceUrl = null), testItem(sourceUrl = resolvedUrl))
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery { resolveDownloadableStreamUseCase(ownerId, mediaId, null, null) } returns
                Async.Success(RankedDownloadCandidate(Stream(name = "1080p", url = resolvedUrl), isHls = false))
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, resolvedUrl, any(), streamFile, any())
            } returns MediaTransferResult.Completed

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateSource(itemId, resolvedUrl, false, 0L) }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null) }
        }

    @Test
    fun `start should seed streamTotalBytes with the probed content length of a resolved direct file`() =
        runTest(testDispatcher) {
            val resolvedUrl = "https://example.com/resolved.mp4"

            coEvery { mediaDownloadRepository.getItem(itemId) } returnsMany
                listOf(testItem(sourceUrl = null), testItem(sourceUrl = resolvedUrl))
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery { resolveDownloadableStreamUseCase(ownerId, mediaId, null, null) } returns
                Async.Success(
                    RankedDownloadCandidate(
                        stream = Stream(name = "1080p", url = resolvedUrl),
                        isHls = false,
                        contentLength = 123_456L,
                    ),
                )
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, resolvedUrl, any(), streamFile, any())
            } returns MediaTransferResult.Completed

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateSource(itemId, resolvedUrl, false, 123_456L) }
        }

    @Test
    fun `start should mark item FAILED when resolution finds nothing reachable and never persisted a source url`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem(sourceUrl = null)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery { resolveDownloadableStreamUseCase(ownerId, mediaId, null, null) } returns
                Async.Failure(UiText.from("nothing reachable"))

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.markError(itemId, "nothing reachable") }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.FAILED, null) }
            coVerify(exactly = 0) { mediaDownloadRepository.runTransfer(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `start should fetch subtitles after the stream completes when valid cached subtitles exist`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed
            coEvery { mediaLinksRepository.getLinks(ownerId, mediaId, null, null) } returns
                listOf(MediaLinksWithData(media = media, subtitles = listOf(cachedSubtitle("en", "https://s/en.srt"))))
            coEvery {
                mediaDownloadRepository.runTransfer(
                    itemId,
                    DownloadPhase.SUBTITLES,
                    "https://s/en.srt",
                    any(),
                    subtitleFile,
                    any()
                )
            } returns MediaTransferResult.Completed

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.STREAM_COMPLETE, null) }
            coVerify {
                mediaDownloadRepository.updateState(
                    itemId,
                    DownloadItemState.FETCHING_SUBTITLES,
                    DownloadPhase.SUBTITLES
                )
            }
            coVerify { mediaDownloadRepository.setTotalSubtitlesCount(itemId, 1) }
            coVerify { mediaDownloadRepository.incrementDownloadedSubtitlesCount(itemId) }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null) }
        }

    @Test
    fun `start should reach COMPLETED with a partial subtitle count when one of several subtitles fails`() =
        runTest(testDispatcher) {
            val subtitleOk = cachedSubtitle("en", "https://s/en.srt")
            val subtitleFailing = cachedSubtitle("es", "https://s/es.srt")

            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed
            coEvery { mediaLinksRepository.getLinks(ownerId, mediaId, null, null) } returns
                listOf(MediaLinksWithData(media = media, subtitles = listOf(subtitleOk, subtitleFailing)))
            coEvery {
                mediaDownloadRepository.runTransfer(
                    itemId,
                    DownloadPhase.SUBTITLES,
                    subtitleOk.url,
                    any(),
                    subtitleFile,
                    any()
                )
            } returns MediaTransferResult.Completed
            coEvery {
                mediaDownloadRepository.runTransfer(
                    itemId,
                    DownloadPhase.SUBTITLES,
                    subtitleFailing.url,
                    any(),
                    subtitleFile,
                    any()
                )
            } returns MediaTransferResult.Failed(IOException("es host down"))

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.setTotalSubtitlesCount(itemId, 2) }
            coVerify(exactly = 1) { mediaDownloadRepository.incrementDownloadedSubtitlesCount(itemId) }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null) }
            coVerify(exactly = 0) { mediaDownloadRepository.updateState(itemId, DownloadItemState.FAILED, any()) }
        }

    @Test
    fun `start should pause between subtitle files when a pause lands after one already completed`() =
        runTest(testDispatcher) {
            // Regression test: each subtitle is its own runTransfer() call, and runTransfer()
            // used to clear any interrupt flag the instant it started — so a pause requested
            // between two (near-instant) subtitle transfers was silently discarded, and the whole
            // subtitle phase ran to completion regardless of the pause tap.
            val subtitle1 = cachedSubtitle("en", "https://s/en.srt")
            val subtitle2 = cachedSubtitle("es", "https://s/es.srt")

            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed
            coEvery { mediaLinksRepository.getLinks(ownerId, mediaId, null, null) } returns
                listOf(MediaLinksWithData(media = media, subtitles = listOf(subtitle1, subtitle2)))
            coEvery {
                mediaDownloadRepository.runTransfer(
                    itemId,
                    DownloadPhase.SUBTITLES,
                    subtitle1.url,
                    any(),
                    subtitleFile,
                    any()
                )
            } returns MediaTransferResult.Completed
            // First null is the stream phase's own pre-transfer checkpoint, second is the one
            // before subtitle1 — then a pause lands right after subtitle1 completes.
            coEvery { mediaDownloadRepository.consumeInterruptReason(itemId) } returnsMany
                listOf(null, null, DownloadInterruptReason.PAUSE)

            controller.start(itemId)
            advanceUntilIdle()

            coVerify(exactly = 1) { mediaDownloadRepository.incrementDownloadedSubtitlesCount(itemId) }
            coVerify(exactly = 0) {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.SUBTITLES, subtitle2.url, any(), any(), any())
            }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.PAUSED, DownloadPhase.SUBTITLES) }
            coVerify(exactly = 0) { mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null) }
        }

    @Test
    fun `start should mark item FAILED when the stream transfer fails and no other cached link is reachable`() =
        runTest(testDispatcher) {
            val primaryUrl = "https://example.com/stream.mp4"
            val clearedItem = testItem(sourceUrl = null, streamFilePath = null)

            coEvery { mediaDownloadRepository.getItem(itemId) } returnsMany
                listOf(testItem(), clearedItem, clearedItem)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, primaryUrl, any(), streamFile, any())
            } returns MediaTransferResult.Failed(IOException("boom"))
            coEvery { resolveDownloadableStreamUseCase(ownerId, mediaId, null, null) } returns
                Async.Failure(UiText.from("boom"))

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaLinksRepository.setLinkStatus(primaryUrl, ownerId, isDead = true) }
            coVerify { mediaDownloadRepository.markError(itemId, "boom") }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.FAILED, null) }
        }

    @Test
    fun `start should mark the dead link, re-resolve, and resume on the runner-up when the stream transfer fails`() =
        runTest(testDispatcher) {
            val primaryUrl = "https://example.com/stream.mp4"
            val fallbackUrl = "https://example.com/fallback.mp4"
            val clearedItem = testItem(sourceUrl = null, streamFilePath = null)
            val resolvedItem = testItem(sourceUrl = fallbackUrl, streamFilePath = null)

            coEvery { mediaDownloadRepository.getItem(itemId) } returnsMany
                listOf(testItem(), clearedItem, resolvedItem)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, primaryUrl, any(), streamFile, any())
            } returns MediaTransferResult.Failed(IOException("boom"))
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, fallbackUrl, any(), streamFile, any())
            } returns MediaTransferResult.Completed
            coEvery { resolveDownloadableStreamUseCase(ownerId, mediaId, null, null) } returns
                Async.Success(RankedDownloadCandidate(Stream(name = "fallback", url = fallbackUrl), isHls = false))

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaLinksRepository.setLinkStatus(primaryUrl, ownerId, isDead = true) }
            coVerify { mediaDownloadRepository.updateSource(itemId, null, false, 0L) }
            coVerify { mediaDownloadRepository.updateSource(itemId, fallbackUrl, false, 0L) }
            coVerify {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, fallbackUrl, any(), streamFile, any())
            }
            coVerify(exactly = 0) { mediaDownloadRepository.updateState(itemId, DownloadItemState.FAILED, any()) }
        }

    @Test
    fun `start should download via the HLS transfer engine when the item is an HLS stream`() =
        runTest(testDispatcher) {
            val segments = listOf(
                HlsSegmentInfo(
                    url = "https://example.com/0.ts",
                    byteRangeOffset = 0,
                    byteRangeLength = -1,
                    encryptionKeyUri = null,
                    encryptionIv = null
                )
            )
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem(isHlsStream = true)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                hlsManifestResolver.resolve("https://example.com/stream.mp4", emptyMap(), any())
            } returns HlsResolutionResult.Success(ResolvedHlsPlaylist(segments))
            coEvery {
                mediaDownloadRepository.runHlsTransfer(itemId, segments, 0, emptyMap(), streamFile)
            } returns MediaTransferResult.Completed

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.runHlsTransfer(itemId, segments, 0, emptyMap(), streamFile) }
            coVerify(exactly = 0) { mediaDownloadRepository.runTransfer(any(), any(), any(), any(), any(), any()) }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null) }
        }

    @Test
    fun `start should resume an HLS download from its persisted segment count`() =
        runTest(testDispatcher) {
            val segments = List(5) {
                HlsSegmentInfo(
                    url = "https://example.com/$it.ts",
                    byteRangeOffset = 0,
                    byteRangeLength = -1,
                    encryptionKeyUri = null,
                    encryptionIv = null
                )
            }
            coEvery {
                mediaDownloadRepository.getItem(itemId)
            } returns testItem(isHlsStream = true, streamBytesDownloaded = 3)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                hlsManifestResolver.resolve("https://example.com/stream.mp4", emptyMap(), any())
            } returns HlsResolutionResult.Success(ResolvedHlsPlaylist(segments))
            coEvery {
                mediaDownloadRepository.runHlsTransfer(itemId, segments, 3, emptyMap(), streamFile)
            } returns MediaTransferResult.Completed

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.runHlsTransfer(itemId, segments, 3, emptyMap(), streamFile) }
        }

    @Test
    fun `start should fall through to the next candidate when HLS resolution fails`() =
        runTest(testDispatcher) {
            val clearedItem = testItem(sourceUrl = null, isHlsStream = false, streamFilePath = null)

            coEvery { mediaDownloadRepository.getItem(itemId) } returnsMany
                listOf(testItem(isHlsStream = true), clearedItem, clearedItem)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                hlsManifestResolver.resolve("https://example.com/stream.mp4", emptyMap(), any())
            } returns HlsResolutionResult.Failed("manifest not found")
            coEvery { resolveDownloadableStreamUseCase(ownerId, mediaId, null, null) } returns
                Async.Failure(UiText.from("manifest not found"))

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.markError(itemId, "manifest not found") }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.FAILED, null) }
        }

    @Test
    fun `start should mark item FAILED when a completed transfer produced a suspiciously small file`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed
            every { streamFile.length() } returns 10L

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.FAILED, null) }
            coVerify(
                exactly = 0
            ) { mediaDownloadRepository.updateState(itemId, DownloadItemState.STREAM_COMPLETE, any()) }
        }

    @Test
    fun `start should mark item PAUSED when interrupted by a pause request`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Cancelled
            coEvery { mediaDownloadRepository.consumeInterruptReason(itemId) } returns DownloadInterruptReason.PAUSE

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.PAUSED, DownloadPhase.STREAM) }
            coVerify(exactly = 0) { directory.delete() }
        }

    @Test
    fun `start should mark item STOPPED and delete the directory when interrupted by a stop request`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Cancelled
            coEvery { mediaDownloadRepository.consumeInterruptReason(itemId) } returns DownloadInterruptReason.STOP

            controller.start(itemId)
            advanceUntilIdle()

            coVerify { directory.delete() }
            coVerify { mediaDownloadRepository.resetChunks(itemId) }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.STOPPED, null) }
        }

    @Test
    fun `start should resume directly into the subtitle phase when the item was paused mid-subtitle`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns
                testItem(
                    state = DownloadItemState.PAUSED,
                    phase = DownloadPhase.SUBTITLES,
                    downloadedSubtitlesCount = 1,
                    totalSubtitlesCount = 2,
                )
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            val remainingSubtitle = cachedSubtitle("es", "https://s/es.srt")
            coEvery { mediaLinksRepository.getLinks(ownerId, mediaId, null, null) } returns
                listOf(
                    MediaLinksWithData(
                        media = media,
                        subtitles = listOf(cachedSubtitle("en", "https://s/en.srt"), remainingSubtitle),
                    )
                )
            coEvery {
                mediaDownloadRepository.runTransfer(
                    itemId,
                    DownloadPhase.SUBTITLES,
                    remainingSubtitle.url,
                    any(),
                    subtitleFile,
                    any()
                )
            } returns MediaTransferResult.Completed

            controller.start(itemId)
            advanceUntilIdle()

            coVerify(
                exactly = 0
            ) { mediaDownloadRepository.updateState(itemId, DownloadItemState.DOWNLOADING_STREAM, any()) }
            coVerify(
                exactly = 0
            ) {
                mediaDownloadRepository.runTransfer(
                    itemId,
                    DownloadPhase.SUBTITLES,
                    "https://s/en.srt",
                    any(),
                    any(),
                    any()
                )
            }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null) }
        }

    @Test
    fun `start should resume into the subtitle phase for an item requeued as QUEUED with the SUBTITLES phase`() =
        runTest(testDispatcher) {
            // The shape resumeInterrupted() leaves a STREAM_COMPLETE/FETCHING_SUBTITLES row in: the
            // video is already fully written, so re-entering the stream phase would refetch all of it.
            coEvery { mediaDownloadRepository.getItem(itemId) } returns
                testItem(
                    state = DownloadItemState.QUEUED,
                    phase = DownloadPhase.SUBTITLES,
                    downloadedSubtitlesCount = 0,
                    totalSubtitlesCount = 1,
                )
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            val subtitle = cachedSubtitle("en", "https://s/en.srt")
            coEvery { mediaLinksRepository.getLinks(ownerId, mediaId, null, null) } returns
                listOf(MediaLinksWithData(media = media, subtitles = listOf(subtitle)))
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.SUBTITLES, subtitle.url, any(), any(), any())
            } returns MediaTransferResult.Completed

            controller.start(itemId)
            advanceUntilIdle()

            coVerify(
                exactly = 0
            ) { mediaDownloadRepository.updateState(itemId, DownloadItemState.DOWNLOADING_STREAM, any()) }
            coVerify(
                exactly = 0
            ) { mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), any(), any()) }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null) }
        }

    @Test
    fun `retry should reset chunks and requeue before restarting the download`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem(state = DownloadItemState.FAILED)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), streamFile, any())
            } returns MediaTransferResult.Completed

            controller.retry(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.resetChunks(itemId) }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.QUEUED, null) }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null) }
        }

    @Test
    fun `delete should remove the directory and the persisted item`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory

            controller.delete(itemId)
            advanceUntilIdle()

            coVerify { directory.delete() }
            coVerify { mediaDownloadRepository.delete(itemId) }
        }

    @Test
    fun `pause on a queued item should transition it directly to PAUSED without an interrupt flag`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem(state = DownloadItemState.QUEUED)

            controller.pause(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.PAUSED, null) }
            coVerify(exactly = 0) { mediaDownloadRepository.requestInterrupt(any(), any()) }
        }

    @Test
    fun `stop on a queued item should clean up and mark STOPPED directly without an interrupt flag`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem(state = DownloadItemState.QUEUED)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory

            controller.stop(itemId)
            advanceUntilIdle()

            coVerify { directory.delete() }
            coVerify { mediaDownloadRepository.resetChunks(itemId) }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.STOPPED, null) }
            coVerify(exactly = 0) { mediaDownloadRepository.requestInterrupt(any(), any()) }
        }

    @Test
    fun `stop on a paused item should clean up and mark STOPPED directly`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns
                testItem(state = DownloadItemState.PAUSED, phase = DownloadPhase.STREAM)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory

            controller.stop(itemId)
            advanceUntilIdle()

            coVerify { directory.delete() }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.STOPPED, null) }
        }

    @Test
    fun `pause on an actively downloading item should request an interrupt instead of transitioning state directly`() =
        runTest(testDispatcher) {
            startGatedTransfer()

            controller.pause(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.requestInterrupt(itemId, DownloadInterruptReason.PAUSE) }
            coVerify(exactly = 0) { mediaDownloadRepository.updateState(itemId, DownloadItemState.PAUSED, any()) }
        }

    @Test
    fun `stop on an actively downloading item should request an interrupt instead of transitioning state directly`() =
        runTest(testDispatcher) {
            startGatedTransfer()

            controller.stop(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.requestInterrupt(itemId, DownloadInterruptReason.STOP) }
            coVerify(exactly = 0) { mediaDownloadRepository.updateState(itemId, DownloadItemState.STOPPED, any()) }
        }

    @Test
    fun `stop on an item left DOWNLOADING_STREAM by a dead process should clean up and mark STOPPED`() =
        runTest(testDispatcher) {
            // Nothing was ever dispatched for this id, so no transfer is polling the interrupt flag —
            // routing the stop through one would leave the row frozen as DOWNLOADING_STREAM forever.
            coEvery { mediaDownloadRepository.getItem(itemId) } returns
                testItem(state = DownloadItemState.DOWNLOADING_STREAM, phase = DownloadPhase.STREAM)
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory

            controller.stop(itemId)
            advanceUntilIdle()

            coVerify { directory.delete() }
            coVerify { mediaDownloadRepository.resetChunks(itemId) }
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.STOPPED, null) }
            coVerify(exactly = 0) { mediaDownloadRepository.requestInterrupt(any(), any()) }
        }

    @Test
    fun `pause on an item left FETCHING_SUBTITLES by a dead process should transition it to PAUSED directly`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getItem(itemId) } returns
                testItem(state = DownloadItemState.FETCHING_SUBTITLES, phase = DownloadPhase.SUBTITLES)

            controller.pause(itemId)
            advanceUntilIdle()

            // Keeps the phase, so resuming picks the subtitles back up instead of the whole video.
            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.PAUSED, DownloadPhase.SUBTITLES) }
            coVerify(exactly = 0) { mediaDownloadRepository.requestInterrupt(any(), any()) }
        }

    @Test
    fun `pause on an orphaned STREAM_COMPLETE item should record the subtitle phase to resume at`() =
        runTest(testDispatcher) {
            // STREAM_COMPLETE carries no phase of its own, so pausing it verbatim would resume into
            // the stream phase and refetch a video that is already fully written.
            coEvery { mediaDownloadRepository.getItem(itemId) } returns
                testItem(state = DownloadItemState.STREAM_COMPLETE, phase = null)

            controller.pause(itemId)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState(itemId, DownloadItemState.PAUSED, DownloadPhase.SUBTITLES) }
        }

    @Test
    fun `resumeInterrupted should requeue the items a dead process left behind and dispatch them`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.requeueInterruptedItems(any()) } returns 1
            coEvery { mediaDownloadRepository.getOldestQueuedItem() } returns testItem() andThen null
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), any(), any())
            } returns MediaTransferResult.Completed

            controller.resumeInterrupted()
            advanceUntilIdle()

            // Nothing is in flight at process start, so no id is shielded from the sweep.
            coVerify { mediaDownloadRepository.requeueInterruptedItems(emptyList()) }
            coVerify {
                mediaDownloadRepository.updateState(itemId, DownloadItemState.DOWNLOADING_STREAM, DownloadPhase.STREAM)
            }
        }

    @Test
    fun `resumeInterrupted should do nothing on a metered connection when wifi-only is on`() =
        runTest(testDispatcher) {
            setWifiOnly(true)
            setMetered(true)
            coEvery { mediaDownloadRepository.requeueInterruptedItems(any()) } returns 1

            controller.resumeInterrupted()
            advanceUntilIdle()

            coVerify(exactly = 0) { mediaDownloadRepository.requeueInterruptedItems(any()) }
        }

    @Test
    fun `resumeInterrupted should sweep on a metered connection when wifi-only is off`() =
        runTest(testDispatcher) {
            setWifiOnly(false)
            setMetered(true)
            coEvery { mediaDownloadRepository.requeueInterruptedItems(any()) } returns 0

            controller.resumeInterrupted()
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.requeueInterruptedItems(any()) }
        }

    @Test
    fun `an explicit start should still run on a metered connection when wifi-only is on`() =
        runTest(testDispatcher) {
            // The whole point of the gate being auto-only: a tap must never be silently swallowed.
            setWifiOnly(true)
            setMetered(true)
            coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), any(), any())
            } returns MediaTransferResult.Completed

            controller.start(itemId)
            advanceUntilIdle()

            coVerify {
                mediaDownloadRepository.updateState(itemId, DownloadItemState.DOWNLOADING_STREAM, DownloadPhase.STREAM)
            }
        }

    @Test
    fun `resumeInterrupted should leave an item that is already being transferred alone`() =
        runTest(testDispatcher) {
            startGatedTransfer()

            controller.resumeInterrupted()
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.requeueInterruptedItems(listOf(itemId)) }
        }

    /**
     * Starts [itemId] and parks it mid-transfer, so it is genuinely dispatched — the state both
     * interrupt paths actually branch on, and the one a plain DB row can't stand in for.
     */
    private suspend fun TestScope.startGatedTransfer() {
        coEvery { mediaDownloadRepository.getItem(itemId) } returns testItem()
        coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
        coEvery {
            mediaDownloadRepository.runTransfer(itemId, DownloadPhase.STREAM, any(), any(), any(), any())
        } coAnswers {
            CompletableDeferred<Unit>().await()
            MediaTransferResult.Completed
        }

        controller.start(itemId)
        advanceUntilIdle()

        coEvery { mediaDownloadRepository.getItem(itemId) } returns
            testItem(state = DownloadItemState.DOWNLOADING_STREAM, phase = DownloadPhase.STREAM)
    }

    @Test
    fun `pauseBatch should pause every item returned for that media and season`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getBatch(mediaId, 1) } returns
                listOf(
                    testItem(id = "item-1", state = DownloadItemState.QUEUED),
                    testItem(id = "item-2", state = DownloadItemState.QUEUED)
                )
            coEvery { mediaDownloadRepository.getItem("item-1") } returns testItem(id = "item-1")
            coEvery { mediaDownloadRepository.getItem("item-2") } returns testItem(id = "item-2")

            controller.pauseBatch(mediaId, 1)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState("item-1", DownloadItemState.PAUSED, null) }
            coVerify { mediaDownloadRepository.updateState("item-2", DownloadItemState.PAUSED, null) }
        }

    @Test
    fun `stopBatch should stop every item returned for that media and season`() =
        runTest(testDispatcher) {
            coEvery { mediaDownloadRepository.getBatch(mediaId, 1) } returns
                listOf(
                    testItem(id = "item-1", state = DownloadItemState.QUEUED),
                    testItem(id = "item-2", state = DownloadItemState.QUEUED)
                )
            coEvery { mediaDownloadRepository.getItem("item-1") } returns testItem(id = "item-1")
            coEvery { mediaDownloadRepository.getItem("item-2") } returns testItem(id = "item-2")
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory

            controller.stopBatch(mediaId, 1)
            advanceUntilIdle()

            coVerify { mediaDownloadRepository.updateState("item-1", DownloadItemState.STOPPED, null) }
            coVerify { mediaDownloadRepository.updateState("item-2", DownloadItemState.STOPPED, null) }
        }

    @Test
    fun `a second item should not start until the first releases its concurrency slot`() =
        runTest(testDispatcher) {
            setConcurrencyLimit(1)

            val item1Gate = CompletableDeferred<Unit>()

            coEvery { mediaDownloadRepository.getItem("item-1") } returns testItem(id = "item-1")
            coEvery { mediaDownloadRepository.getItem("item-2") } returns testItem(id = "item-2")
            coEvery { getDownloadDirectoryUseCase(any(), any(), any(), any()) } returns directory
            coEvery {
                mediaDownloadRepository.runTransfer("item-1", DownloadPhase.STREAM, any(), any(), any(), any())
            } coAnswers {
                item1Gate.await()
                MediaTransferResult.Completed
            }
            coEvery {
                mediaDownloadRepository.runTransfer("item-2", DownloadPhase.STREAM, any(), any(), any(), any())
            } returns MediaTransferResult.Completed
            coEvery { mediaDownloadRepository.getOldestQueuedItem() } returns testItem(id = "item-2") andThen null

            controller.start("item-1")
            controller.start("item-2")
            advanceUntilIdle()

            // Item 1 is still mid-transfer (blocked on item1Gate), so item 2 must not have gotten a slot.
            coVerify(exactly = 1) {
                mediaDownloadRepository.updateState(
                    "item-1",
                    DownloadItemState.DOWNLOADING_STREAM,
                    DownloadPhase.STREAM
                )
            }
            coVerify(
                exactly = 0
            ) { mediaDownloadRepository.updateState("item-2", DownloadItemState.DOWNLOADING_STREAM, any()) }

            item1Gate.complete(Unit)
            advanceUntilIdle()

            // Once item 1 finishes and frees its slot, dispatchNext() should pick item 2 up.
            coVerify(exactly = 1) {
                mediaDownloadRepository.updateState(
                    "item-2",
                    DownloadItemState.DOWNLOADING_STREAM,
                    DownloadPhase.STREAM
                )
            }
        }
}
