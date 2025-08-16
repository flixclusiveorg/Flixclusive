package com.flixclusive.domain.database.usecase.impl

import com.flixclusive.core.database.entity.watchlist.Watchlist
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.model.film.Film
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Date

class ToggleWatchlistStatusUseCaseImplTest {

    private val watchlistRepository: WatchlistRepository = mockk(relaxed = true)
    private lateinit var useCase: ToggleWatchlistStatusUseCaseImpl

    private val testWatchlist = Watchlist(
        id = 1L,
        filmId = "test-film-id",
        ownerId = 123,
        addedAt = Date(),
    )

    private val testFilm: Film = mockk()

    @Before
    fun setUp() {
        useCase = ToggleWatchlistStatusUseCaseImpl(watchlistRepository)
    }

    @Test
    fun `invoke removes watchlist item when already exists in watchlist`() = runTest {
        coEvery {
            watchlistRepository.isInWatchlist(
                filmId = testWatchlist.filmId,
                ownerId = testWatchlist.ownerId,
            )
        } returns true

        useCase.invoke(testWatchlist, testFilm)

        coVerify { watchlistRepository.remove(id = testWatchlist.id) }
        coVerify(exactly = 0) {
            watchlistRepository.insert(item = any(), film = any())
        }
    }

    @Test
    fun `invoke adds watchlist item when not in watchlist`() = runTest {
        coEvery {
            watchlistRepository.isInWatchlist(
                filmId = testWatchlist.filmId,
                ownerId = testWatchlist.ownerId,
            )
        } returns false

        useCase.invoke(testWatchlist, testFilm)

        coVerify { watchlistRepository.insert(item = testWatchlist, film = testFilm) }
        coVerify(exactly = 0) { watchlistRepository.remove(id = any()) }
    }

    @Test
    fun `invoke adds watchlist item with null film when not in watchlist`() = runTest {
        coEvery {
            watchlistRepository.isInWatchlist(
                filmId = testWatchlist.filmId,
                ownerId = testWatchlist.ownerId,
            )
        } returns false

        useCase.invoke(testWatchlist, null)

        coVerify { watchlistRepository.insert(item = testWatchlist, film = null) }
        coVerify(exactly = 0) { watchlistRepository.remove(id = any()) }
    }

    @Test
    fun `invoke uses correct parameters when checking if item is in watchlist`() = runTest {
        coEvery {
            watchlistRepository.isInWatchlist(
                filmId = testWatchlist.filmId,
                ownerId = testWatchlist.ownerId,
            )
        } returns false

        useCase.invoke(testWatchlist, testFilm)

        coVerify {
            watchlistRepository.isInWatchlist(
                filmId = testWatchlist.filmId,
                ownerId = testWatchlist.ownerId,
            )
        }
    }
}
