package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.domain.downloads.usecase.ResolveDownloadableStreamUseCase
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import io.mockk.coEvery
import io.mockk.coVerify
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
    private lateinit var resolveDownloadableStreamUseCase: ResolveDownloadableStreamUseCase
    private lateinit var mediaDownloadRepository: MediaDownloadRepository
    private lateinit var useCase: QueueMediaDownloadUseCaseImpl

    private val testMovie = Movie(
        id = "123",
        title = "Test Movie",
        providerId = "test-provider",
        posterImage = null,
    )

    private val testStream = Stream(name = "1080p", url = "https://example.com/stream.mp4")

    @Before
    fun setup() {
        resolveDownloadableStreamUseCase = mockk()
        mediaDownloadRepository = mockk()

        useCase = QueueMediaDownloadUseCaseImpl(resolveDownloadableStreamUseCase, mediaDownloadRepository)
    }

    @Test
    fun `invoke should return failure without queuing when resolution fails`() =
        runTest {
            coEvery { resolveDownloadableStreamUseCase(any()) } returns Async.Failure(UiText.from("no links"))

            val result = useCase(testMovie, null, listOf(testStream), null)

            expectThat(result).isA<Async.Failure>()
            coVerify(exactly = 0) { mediaDownloadRepository.queue(any()) }
        }

    @Test
    fun `invoke should queue a download item using the resolved stream for a movie`() =
        runTest {
            coEvery { resolveDownloadableStreamUseCase(any()) } returns Async.Success(testStream)
            val itemSlot = slot<DownloadItem>()
            coEvery { mediaDownloadRepository.queue(capture(itemSlot)) } returns 7L

            val result = useCase(testMovie, null, listOf(testStream), null)

            expectThat(result).isA<Async.Success<Long>>().get { data }.isEqualTo(7L)
            expectThat(itemSlot.captured.mediaId).isEqualTo("123")
            expectThat(itemSlot.captured.mediaTitle).isEqualTo("Test Movie")
            expectThat(itemSlot.captured.mediaType).isEqualTo(MediaType.MOVIE)
            expectThat(itemSlot.captured.streamUrl).isEqualTo(testStream.url)
            expectThat(itemSlot.captured.subtitleUrl).isNull()
        }

    @Test
    fun `invoke should persist episode and subtitle info when provided`() =
        runTest {
            val episode = Episode(id = "ep-1", number = 2, season = 1, isReleased = true, title = "Pilot")
            val subtitle = Subtitle(language = "en", url = "https://example.com/subs.srt")

            coEvery { resolveDownloadableStreamUseCase(any()) } returns Async.Success(testStream)
            val itemSlot = slot<DownloadItem>()
            coEvery { mediaDownloadRepository.queue(capture(itemSlot)) } returns 1L

            useCase(testMovie, episode, listOf(testStream), subtitle)

            expectThat(itemSlot.captured.seasonNumber).isEqualTo(1)
            expectThat(itemSlot.captured.episodeNumber).isEqualTo(2)
            expectThat(itemSlot.captured.episodeTitle).isEqualTo("Pilot")
            expectThat(itemSlot.captured.subtitleUrl).isEqualTo(subtitle.url)
        }
}
