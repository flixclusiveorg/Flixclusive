package com.flixclusive.domain.provider.usecase.download.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.domain.downloads.controller.MediaDownloadController
import com.flixclusive.domain.downloads.usecase.QueueMediaDownloadBatchUseCase
import com.flixclusive.domain.downloads.usecase.QueueMediaDownloadUseCase
import com.flixclusive.domain.provider.usecase.download.DownloadTarget
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA

class ToggleMediaDownloadUseCaseImplTest {
    private lateinit var mediaDownloadRepository: MediaDownloadRepository
    private lateinit var mediaDownloadController: MediaDownloadController
    private lateinit var queueMediaDownload: QueueMediaDownloadUseCase
    private lateinit var queueMediaDownloadBatch: QueueMediaDownloadBatchUseCase
    private lateinit var getMediaLinks: GetMediaLinksUseCase
    private lateinit var userSessionDataStore: UserSessionDataStore
    private lateinit var useCase: ToggleMediaDownloadUseCaseImpl

    private val ownerId = "owner-1"

    private val movie = Movie(
        id = "movie-1",
        title = "Test Movie",
        providerId = "test-provider",
        posterImage = null,
    )

    private val show = Show(
        id = "show-1",
        title = "Test Show",
        providerId = "test-provider",
        posterImage = null,
        seasons = emptyList(),
        totalEpisodes = 2,
        totalSeasons = 1,
    )

    private fun episode(number: Int) = Episode(id = "ep-$number", number = number, season = 1, isReleased = true)

    private fun season() = Season.Full(
        id = "show-1-s1",
        number = 1,
        isReleased = true,
        episodes = listOf(episode(1), episode(2)),
    )

    private fun item(
        id: String,
        state: DownloadItemState,
        episodeNumber: Int? = null,
    ) = DownloadItem(
        id = id,
        ownerId = ownerId,
        mediaId = if (episodeNumber == null) movie.id else show.id,
        mediaTitle = "Test",
        mediaType = if (episodeNumber == null) MediaType.MOVIE else MediaType.SHOW,
        seasonNumber = if (episodeNumber == null) null else 1,
        episodeNumber = episodeNumber,
        state = state,
    )

    @Before
    fun setup() {
        mediaDownloadRepository = mockk(relaxed = true)
        mediaDownloadController = mockk(relaxed = true)
        queueMediaDownload = mockk(relaxed = true)
        queueMediaDownloadBatch = mockk(relaxed = true)
        getMediaLinks = mockk()
        userSessionDataStore = mockk()

        every { userSessionDataStore.currentUserId } returns flowOf(ownerId)
        every { getMediaLinks(any(), any()) } returns flowOf(LoadLinksState.Success)
        coEvery { mediaDownloadRepository.getFor(any(), any(), any()) } returns null
        coEvery { mediaDownloadRepository.getBatch(any(), any()) } returns emptyList()

        useCase = ToggleMediaDownloadUseCaseImpl(
            mediaDownloadRepository = mediaDownloadRepository,
            mediaDownloadController = mediaDownloadController,
            queueMediaDownload = queueMediaDownload,
            queueMediaDownloadBatch = queueMediaDownloadBatch,
            getMediaLinks = getMediaLinks,
            userSessionDataStore = userSessionDataStore,
        )
    }

    @Test
    fun `a movie with no existing download should warm the link cache and queue`() =
        runTest {
            val result = useCase(DownloadTarget.Single(movie))

            expectThat(result).isA<Async.Success<Unit>>()
            verify { getMediaLinks(movie, null) }
            coVerify { queueMediaDownload(movie, null, ownerId) }
        }

    @Test
    fun `a movie whose links cannot be resolved should fail without queueing`() =
        runTest {
            every { getMediaLinks(any(), any()) } returns
                flowOf(LoadLinksState.Error(UiText.from("no links")))

            val result = useCase(DownloadTarget.Single(movie))

            expectThat(result).isA<Async.Failure>()
            coVerify(exactly = 0) { queueMediaDownload(any(), any(), any()) }
        }

    @Test
    fun `an in-flight download should be stopped rather than queued again`() =
        runTest {
            coEvery { mediaDownloadRepository.getFor(movie.id, null, null) } returns
                item("item-1", DownloadItemState.DOWNLOADING_STREAM)

            useCase(DownloadTarget.Single(movie))

            verify { mediaDownloadController.stop("item-1") }
            coVerify(exactly = 0) { queueMediaDownload(any(), any(), any()) }
        }

    @Test
    fun `a failed download should be retried`() =
        runTest {
            coEvery { mediaDownloadRepository.getFor(movie.id, null, null) } returns
                item("item-1", DownloadItemState.FAILED)

            useCase(DownloadTarget.Single(movie))

            verify { mediaDownloadController.retry("item-1") }
        }

    @Test
    fun `a completed download should be left alone`() =
        runTest {
            coEvery { mediaDownloadRepository.getFor(movie.id, null, null) } returns
                item("item-1", DownloadItemState.COMPLETED)

            useCase(DownloadTarget.Single(movie))

            verify(exactly = 0) { mediaDownloadController.stop(any()) }
            verify(exactly = 0) { mediaDownloadController.retry(any()) }
            coVerify(exactly = 0) { queueMediaDownload(any(), any(), any()) }
        }

    @Test
    fun `a season with anything still running should be stopped as a batch`() =
        runTest {
            coEvery { mediaDownloadRepository.getBatch(show.id, 1) } returns
                listOf(item("item-1", DownloadItemState.DOWNLOADING_STREAM, episodeNumber = 1))

            useCase(DownloadTarget.WholeSeason(show, season()))

            verify { mediaDownloadController.stopBatch(show.id, 1) }
            coVerify(exactly = 0) { queueMediaDownloadBatch(any()) }
        }

    @Test
    fun `a season should only queue the episodes that are not already downloaded`() =
        runTest {
            coEvery { mediaDownloadRepository.getBatch(show.id, 1) } returns
                listOf(item("item-1", DownloadItemState.COMPLETED, episodeNumber = 1))

            useCase(DownloadTarget.WholeSeason(show, season()))

            coVerify { queueMediaDownloadBatch(match { it.size == 1 && it.single().episode?.number == 2 }) }
        }
}
