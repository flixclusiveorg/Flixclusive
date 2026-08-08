package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class QueueMediaDownloadUseCaseImplTest {
    private lateinit var mediaDownloadRepository: MediaDownloadRepository
    private lateinit var useCase: QueueMediaDownloadUseCaseImpl

    private val testMovie = Movie(
        id = "123",
        title = "Test Movie",
        providerId = "test-provider",
        posterImage = null,
    )

    private val ownerId = "owner-1"

    @Before
    fun setup() {
        mediaDownloadRepository = mockk()
        coEvery { mediaDownloadRepository.queue(any()) } answers { firstArg<DownloadItem>().id }

        useCase = QueueMediaDownloadUseCaseImpl(mediaDownloadRepository)
    }

    @Test
    fun `invoke should queue a QUEUED item with no source url for a movie`() =
        runTest {
            val itemSlot = slot<DownloadItem>()
            coEvery { mediaDownloadRepository.queue(capture(itemSlot)) } answers { firstArg<DownloadItem>().id }

            val result = useCase(testMovie, null, ownerId)

            expectThat(result).isA<Async.Success<String>>().get { data }.isEqualTo(itemSlot.captured.id)
            expectThat(itemSlot.captured.ownerId).isEqualTo(ownerId)
            expectThat(itemSlot.captured.mediaId).isEqualTo("123")
            expectThat(itemSlot.captured.mediaTitle).isEqualTo("Test Movie")
            expectThat(itemSlot.captured.mediaType).isEqualTo(MediaType.MOVIE)
            expectThat(itemSlot.captured.state).isEqualTo(DownloadItemState.QUEUED)
            expectThat(itemSlot.captured.sourceUrl).isNull()
        }

    @Test
    fun `invoke should persist episode info when provided`() =
        runTest {
            val episode = Episode(id = "ep-1", number = 2, season = 1, isReleased = true, title = "Pilot")
            val itemSlot = slot<DownloadItem>()
            coEvery { mediaDownloadRepository.queue(capture(itemSlot)) } answers { firstArg<DownloadItem>().id }

            useCase(testMovie, episode, ownerId)

            expectThat(itemSlot.captured.seasonNumber).isEqualTo(1)
            expectThat(itemSlot.captured.episodeNumber).isEqualTo(2)
        }
}
