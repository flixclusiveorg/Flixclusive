package com.flixclusive.domain.database.usecase.impl

import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class ToggleWatchProgressStatusUseCaseImplTest {
    private val repository: WatchProgressRepository = mockk(relaxed = true)
    private val session: UserSessionManager = mockk(relaxed = true)
    private lateinit var useCase: ToggleWatchProgressStatusUseCaseImpl
    private val testDispatcher = StandardTestDispatcher()

    private val user = User(id = 1, name = "User", image = 0)

    @Before
    fun setUp() {
        every { session.currentUser } returns MutableStateFlow(user)
        useCase = ToggleWatchProgressStatusUseCaseImpl(repository, session)
    }

    @Test
    fun `given no user when invoking then throws`() =
        runTest(testDispatcher) {
            every { session.currentUser } returns MutableStateFlow(null)
            val movie = FilmTestDefaults.getMovie()

            val error = expectCatching { useCase.invoke(movie) }
                .isFailure()
                .isA<IllegalArgumentException>()

            advanceUntilIdle()

            // Using message ensures expected guard is hit
            expectThat(error.subject.message)
                .isNotNull()
                .contains("User must be logged in to toggle watch progress")
        }

    @Test
    fun `given movie and no existing progress when invoking then inserts completed movie progress`() =
        runTest(testDispatcher) {
            val movie = FilmTestDefaults.getMovie()
            every {
                repository.getAsFlow(
                    id = movie.identifier,
                    ownerId = user.id,
                    type = movie.filmType,
                )
            } returns flowOf(null)
            coEvery { repository.insert(any(), movie) } returns 1L

            val captured: CapturingSlot<com.flixclusive.core.database.entity.watched.WatchProgress> = slot()

            useCase.invoke(movie)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.insert(capture(captured), movie) }
            val inserted = captured.captured as MovieProgress

            expectThat(movie.identifier).isEqualTo(inserted.filmId)
            expectThat(user.id).isEqualTo(inserted.ownerId)
            expectThat(0L).isEqualTo(inserted.progress)
            expectThat(WatchStatus.COMPLETED).isEqualTo(inserted.status)
        }

    @Test
    fun `given tv show and no existing progress when invoking then inserts completed episode progress`() =
        runTest(testDispatcher) {
            val show = FilmTestDefaults.getTvShow()
            every {
                repository.getAsFlow(
                    id = show.identifier,
                    ownerId = user.id,
                    type = show.filmType,
                )
            } returns flowOf(null)
            coEvery { repository.insert(any(), show) } returns 1L

            val captured: CapturingSlot<com.flixclusive.core.database.entity.watched.WatchProgress> = slot()

            useCase.invoke(show)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.insert(capture(captured), show) }
            val inserted = captured.captured as EpisodeProgress

            expectThat(show.identifier).isEqualTo(inserted.filmId)
            expectThat(user.id).isEqualTo(inserted.ownerId)
            expectThat(0L).isEqualTo(inserted.progress)
            expectThat(WatchStatus.COMPLETED).isEqualTo(inserted.status)
            expectThat(show.totalSeasons).isEqualTo(inserted.seasonNumber)
            expectThat(show.totalEpisodes).isEqualTo(inserted.episodeNumber)
        }

    @Test
    fun `given existing progress when invoking then deletes progress`() =
        runTest(testDispatcher) {
            val movie = FilmTestDefaults.getMovie()
            val dbFilm = DBFilm(id = movie.identifier, title = movie.title)
            val existing = MovieProgress(
                id = 99L,
                filmId = movie.identifier,
                ownerId = user.id,
                progress = 500L,
                status = WatchStatus.WATCHING,
            )
            every {
                repository.getAsFlow(
                    id = movie.identifier,
                    ownerId = user.id,
                    type = movie.filmType,
                )
            } returns flowOf(
                MovieProgressWithMetadata(
                    watchData = existing,
                    film = dbFilm,
                ),
            )
            coEvery { repository.delete(existing.id, movie.filmType) } returns Unit

            useCase.invoke(movie)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.delete(existing.id, movie.filmType) }
        }
}
