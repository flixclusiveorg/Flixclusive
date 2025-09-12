package com.flixclusive.domain.database.usecase.impl

import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watchlist.Watchlist
import com.flixclusive.core.database.entity.watchlist.WatchlistWithMetadata
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.model.film.Film
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import java.util.Date

class ToggleWatchlistStatusUseCaseImplTest {

    private lateinit var watchlistRepository: WatchlistRepository
    private lateinit var userSessionManager: UserSessionManager
    private lateinit var useCase: ToggleWatchlistStatusUseCaseImpl

    private val testFilm: Film = FilmTestDefaults.getMovie()
    private val user = User(id = 1, name = "User", image = 0)

    private val testWatchlist = Watchlist(
        id = 1L,
        filmId = testFilm.identifier,
        ownerId = user.id,
        addedAt = Date(),
    )

    @Before
    fun setUp() {
        watchlistRepository = mockk(relaxed = true)
        userSessionManager = mockk(relaxed = true) {
            every { currentUser } returns MutableStateFlow(user).asStateFlow()
        }
        useCase = ToggleWatchlistStatusUseCaseImpl(watchlistRepository, userSessionManager)
    }

    @Test
    fun `invoke removes watchlist item when already exists in watchlist`() = runTest {
        coEvery {
            watchlistRepository.get(
                filmId = testFilm.identifier,
                ownerId = user.id,
            )
        } returns WatchlistWithMetadata(
            watchlist = testWatchlist,
            film = testFilm.toDBFilm(),
        )

        useCase.invoke(testFilm)

        coVerify { watchlistRepository.remove(id = testWatchlist.id) }
        coVerify(exactly = 0) {
            watchlistRepository.insert(item = any(), film = any())
        }
    }

    @Test
    fun `invoke adds watchlist item when not in watchlist`() = runTest {
        coEvery {
            watchlistRepository.get(
                filmId = testFilm.identifier,
                ownerId = user.id,
            )
        } returns null

        useCase.invoke(testFilm)

        coVerify {
            watchlistRepository.insert(
                item = match { it.filmId == testFilm.identifier && it.ownerId == user.id },
                film = testFilm
            )
        }
        coVerify(exactly = 0) { watchlistRepository.remove(id = any()) }
    }

    @Test
    fun `invoke uses correct parameters when checking if item is in watchlist`() = runTest {
        coEvery {
            watchlistRepository.get(
                filmId = testFilm.identifier,
                ownerId = user.id,
            )
        } returns null

        useCase.invoke(testFilm)

        coVerify {
            watchlistRepository.get(
                filmId = testFilm.identifier,
                ownerId = user.id,
            )
        }
    }

    @Test
    fun `invoke throws exception when user is not logged in`() = runTest {
        every { userSessionManager.currentUser } returns MutableStateFlow(null).asStateFlow()

        val error = expectCatching { useCase.invoke(testFilm) }
            .isFailure()
            .isA<IllegalArgumentException>()

        advanceUntilIdle()

        // Using message ensures expected guard is hit
        expectThat(error.subject.message)
            .isNotNull()
            .contains("User must be logged in to toggle watch progress")

        coVerify(exactly = 0) { watchlistRepository.get(filmId = any(), ownerId = any()) }
        coVerify(exactly = 0) { watchlistRepository.insert(item = any(), film = any()) }
        coVerify(exactly = 0) { watchlistRepository.remove(id = any()) }
    }
}
