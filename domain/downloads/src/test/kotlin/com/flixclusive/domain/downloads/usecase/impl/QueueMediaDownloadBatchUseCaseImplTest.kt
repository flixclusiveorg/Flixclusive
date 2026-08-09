package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.domain.downloads.model.MediaDownloadRequest
import com.flixclusive.domain.downloads.usecase.QueueMediaDownloadUseCase
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

class QueueMediaDownloadBatchUseCaseImplTest {
    private lateinit var queueMediaDownloadUseCase: QueueMediaDownloadUseCase
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

    private val ownerId = "owner-1"

    private fun episode(number: Int) = Episode(id = "ep-$number", number = number, season = 1, isReleased = true)

    private fun request(number: Int) = MediaDownloadRequest(
        media = testShow,
        episode = episode(number),
        ownerId = ownerId,
    )

    @Before
    fun setup() {
        queueMediaDownloadUseCase = mockk()
        useCase = QueueMediaDownloadBatchUseCaseImpl(queueMediaDownloadUseCase)
    }

    @Test
    fun `invoke should queue every request and return each id`() =
        runTest {
            coEvery { queueMediaDownloadUseCase(testShow, episode(1), ownerId) } returns "item-10"
            coEvery { queueMediaDownloadUseCase(testShow, episode(2), ownerId) } returns "item-20"

            val results = useCase(listOf(request(1), request(2)))

            expectThat(results).containsExactlyInAnyOrder("item-10", "item-20")
            // Starting is the single-item use case's job now, so the batch never does it itself.
            coVerify { queueMediaDownloadUseCase(testShow, episode(1), ownerId) }
            coVerify { queueMediaDownloadUseCase(testShow, episode(2), ownerId) }
        }
}
