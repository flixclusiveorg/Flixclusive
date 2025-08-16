package com.flixclusive.data.database.repository.impl

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

@RunWith(AndroidJUnit4::class)
class WatchlistRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: WatchlistRepositoryImpl
    private lateinit var appDispatchers: AppDispatchers

    private val testDispatcher = StandardTestDispatcher()
    private val testFilm = DatabaseTestDefaults.getDBFilm()
    private val testUser = DatabaseTestDefaults.getUser()
    private val testWatchlistItem = DatabaseTestDefaults.getWatchlistItem(
        filmId = testFilm.id,
        ownerId = testUser.id,
    )

    @Before
    fun setUp() {
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
        database = DatabaseTestDefaults.createDatabase(
            context = ApplicationProvider.getApplicationContext(),
        )
        repository = WatchlistRepositoryImpl(
            database.watchlistDao(),
            appDispatchers = appDispatchers,
        )

        runBlocking {
            database.userDao().insert(testUser)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun shouldInsertWatchlistItem() =
        runTest(testDispatcher) {
            val itemId = repository.insert(testWatchlistItem, testFilm)

            repository.getAllAsFlow(testWatchlistItem.ownerId).test {
                val result = awaitItem()
                expectThat(result).hasSize(1)
                expectThat(result.first()).and {
                    get { id }.isEqualTo(itemId)
                    get { film.title }.isEqualTo(testFilm.title)
                    get { watchlist.ownerId }.isEqualTo(testWatchlistItem.ownerId)
                }
            }
        }

    @Test
    fun shouldRetrieveWatchlistItemById() =
        runTest(testDispatcher) {
            val itemId = repository.insert(testWatchlistItem, testFilm)

            val retrievedItem = repository.get(itemId)

            expectThat(retrievedItem).isNotNull().and {
                get { id }.isEqualTo(itemId)
                get { film.title }.isEqualTo(testFilm.title)
                get { film.filmType }.isEqualTo(testFilm.filmType)
            }
        }

    @Test
    fun shouldGetAllByOwnerId() =
        runTest(testDispatcher) {
            database.userDao().insert(testUser.copy(id = 2))
            val item1 = testWatchlistItem.copy(id = 1)
            val item2 = testWatchlistItem.copy(filmId = "item2")
            val item3 = testWatchlistItem.copy(filmId = "item3", ownerId = 2)

            repository.insert(item1, testFilm)
            repository.insert(item2, testFilm.copy(id = "item2"))
            repository.insert(item3, testFilm.copy(id = "item3"))

            val result = repository.getAll(testWatchlistItem.ownerId)
            expectThat(result).hasSize(2)
        }

    @Test
    fun shouldRemoveWatchlistItem() =
        runTest(testDispatcher) {
            val id = repository.insert(testWatchlistItem, testFilm)

            repository.remove(id)

            val retrievedItem = repository.get(id)
            expectThat(retrievedItem).isNull()
        }

    @Test
    fun shouldRemoveAllWatchlistItemsForOwner() =
        runTest(testDispatcher) {
            database.userDao().insert(testUser.copy(id = 2))
            val item1 = testWatchlistItem.copy(id = 1)
            val item2 = testWatchlistItem.copy(id = 2)
            val item3 = testWatchlistItem.copy(id = 3, ownerId = 2)

            repository.insert(item1, testFilm)
            repository.insert(item2, testFilm)
            repository.insert(item3, testFilm)

            repository.removeAll(testWatchlistItem.ownerId)

            turbineScope {
                val firstOwner = repository.getAllAsFlow(testWatchlistItem.ownerId).testIn(this)
                val secondOwner = repository.getAllAsFlow(2).testIn(this)

                expectThat(firstOwner.awaitItem()).isEmpty()
                expectThat(secondOwner.awaitItem()).hasSize(1)

                firstOwner.cancelAndIgnoreRemainingEvents()
                secondOwner.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun shouldReturnNullForNonExistentItem() =
        runTest(testDispatcher) {
            val retrievedItem = repository.get(999)
            expectThat(retrievedItem).isNull()
        }

    @Test
    fun shouldReturnTrueIfFilmIsInWatchlist() =
        runTest(testDispatcher) {
            repository.insert(testWatchlistItem, testFilm)

            val isInWatchlist = repository.isInWatchlist(
                filmId = testFilm.id,
                ownerId = testWatchlistItem.ownerId,
            )

            expectThat(isInWatchlist).isEqualTo(true)
        }

    @Test
    fun shouldReturnFalseIfFilmIsNotInWatchlist() =
        runTest(testDispatcher) {
            val isInWatchlist = repository.isInWatchlist(
                filmId = "non-existent-film",
                ownerId = testWatchlistItem.ownerId,
            )

            expectThat(isInWatchlist).isEqualTo(false)
        }
}
