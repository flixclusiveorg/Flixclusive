package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.domain.downloads.controller.MediaDownloadController
import com.flixclusive.domain.downloads.model.MediaDownloadRequest
import com.flixclusive.domain.downloads.usecase.QueueMediaDownloadUseCase
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Stream
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA

class QueueMediaDownloadBatchUseCaseImplTest {
    private lateinit var queueMediaDownloadUseCase: QueueMediaDownloadUseCase
    private lateinit var mediaDownloadController: MediaDownloadController
    private lateinit var useCase: QueueMediaDownloadBatchUseCaseImpl

    private val testShow = Show(
        id = "1",
        title = "Test Show",
        providerId = "test-provider",
        posterImage = null,
        seasons = emptyList(),
        totalEpisodes = 2,
        totalSeasons = 1,
    )

    private fun episode(number: Int) = Episode(id = "ep-$number", number = number, season = 1, isReleased = true)

    private fun request(number: Int) = MediaDownloadRequest(
        media = testShow,
        episode = episode(number),
        streams = listOf(Stream(name = "1080p", url = "https://example.com/$number.mp4")),
        subtitle = null,
    )

    @Before
    fun setup() {
        queueMediaDownloadUseCase = mockk()
        mediaDownloadController = mockk(relaxed = true)
        useCase = QueueMediaDownloadBatchUseCaseImpl(queueMediaDownloadUseCase, mediaDownloadController)
    }

    @Test
    fun `invoke should queue every request and start each successfully queued item`() =
        runTest {
            coEvery { queueMediaDownloadUseCase(testShow, episode(1), any(), null) } returns Async.Success(10L)
            coEvery { queueMediaDownloadUseCase(testShow, episode(2), any(), null) } returns Async.Success(20L)

            val results = useCase(listOf(request(1), request(2)))

            expectThat(results).hasSize(2)
            coVerify { mediaDownloadController.start(10L) }
            coVerify { mediaDownloadController.start(20L) }
        }

    @Test
    fun `invoke should not start items whose queuing failed`() =
        runTest {
            coEvery { queueMediaDownloadUseCase(testShow, episode(1), any(), null) } returns Async.Success(10L)
            coEvery { queueMediaDownloadUseCase(testShow, episode(2), any(), null) } returns
                Async.Failure(UiText.from("no links"))

            val results = useCase(listOf(request(1), request(2)))

            expectThat(results[0]).isA<Async.Success<Long>>()
            expectThat(results[1]).isA<Async.Failure>()
            coVerify(exactly = 1) { mediaDownloadController.start(any()) }
            coVerify { mediaDownloadController.start(10L) }
        }
}
