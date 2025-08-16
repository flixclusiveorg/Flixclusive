package com.flixclusive.domain.database.usecase.impl

import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.data.database.repository.WatchProgressRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Date

class SetWatchProgressUseCaseImplTest {
    private val watchProgressRepository: WatchProgressRepository = mockk(relaxed = true)
    private lateinit var useCase: SetWatchProgressUseCaseImpl

    @Before
    fun setUp() {
        useCase = SetWatchProgressUseCaseImpl(watchProgressRepository)
    }

    @Test
    fun `invoke inserts watch progress when progress is more than a minute`() =
        runTest {
            val watchProgress = createTestWatchProgress(progress = 70_000L)

            useCase.invoke(watchProgress)

            coVerify { watchProgressRepository.insert(watchProgress) }
        }

    @Test
    fun `invoke does not insert watch progress when progress is exactly one minute`() =
        runTest {
            val watchProgress = createTestWatchProgress(progress = 60_000L)

            useCase.invoke(watchProgress)

            coVerify(exactly = 0) { watchProgressRepository.insert(any()) }
        }

    @Test
    fun `invoke does not insert watch progress when progress is less than a minute`() =
        runTest {
            val watchProgress = createTestWatchProgress(progress = 30_000L)

            useCase.invoke(watchProgress)

            coVerify(exactly = 0) { watchProgressRepository.insert(any()) }
        }

    @Test
    fun `invoke does not insert watch progress when progress is zero`() =
        runTest {
            val watchProgress = createTestWatchProgress(progress = 0L)

            useCase.invoke(watchProgress)

            coVerify(exactly = 0) { watchProgressRepository.insert(any()) }
        }

    @Test
    fun `invoke inserts watch progress when progress is just over a minute`() =
        runTest {
            val watchProgress = createTestWatchProgress(progress = 60_001L)

            useCase.invoke(watchProgress)

            coVerify { watchProgressRepository.insert(watchProgress) }
        }

    private fun createTestWatchProgress(
        id: Long = 1L,
        filmId: String = "test-film-id",
        ownerId: Int = 123,
        progress: Long = 70_000L,
        status: WatchStatus = WatchStatus.WATCHING,
        duration: Long = 3600_000L,
        watchedAt: Date = Date(),
        watchCount: Int = 1,
    ): WatchProgress {
        return MovieProgress(
            id = id,
            filmId = filmId,
            ownerId = ownerId,
            progress = progress,
            status = status,
            duration = duration,
            watchedAt = watchedAt,
            watchCount = watchCount,
        )
    }
}
