package com.flixclusive.data.database.repository.impl

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import kotlinx.coroutines.runBlocking
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

    private val testWatchlistItem = DatabaseTestDefaults.getWatchlistItem()
    private val testUser = DatabaseTestDefaults.getUser()

    @Before
    fun setUp() {
        database = DatabaseTestDefaults.createDatabase(
            context = ApplicationProvider.getApplicationContext()
        )
        repository = WatchlistRepositoryImpl(database.watchlistDao())

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
        runTest {
            repository.insert(testWatchlistItem)

            repository.getAllItemsInFlow(testWatchlistItem.ownerId).test {
                val result = awaitItem()
                expectThat(result).hasSize(1)
                expectThat(result.first()).and {
                    get { id }.isEqualTo(testWatchlistItem.id)
                    get { film.title }.isEqualTo(testWatchlistItem.film.title)
                    get { ownerId }.isEqualTo(testWatchlistItem.ownerId)
                }
            }
        }

    @Test
    fun shouldRetrieveWatchlistItemById() =
        runTest {
            repository.insert(testWatchlistItem)

            val retrievedItem = repository.getWatchlistItemById(
                testWatchlistItem.id,
                testWatchlistItem.ownerId,
            )

            expectThat(retrievedItem).isNotNull().and {
                get { id }.isEqualTo(testWatchlistItem.id)
                get { film.title }.isEqualTo(testWatchlistItem.film.title)
                get { film.filmType }.isEqualTo(testWatchlistItem.film.filmType)
            }
        }

    @Test
    fun shouldGetAllItemsByOwnerId() =
        runTest {
            database.userDao().insert(testUser.copy(id = 2))
            val item1 = testWatchlistItem.copy(id = "movie_1")
            val item2 = testWatchlistItem.copy(id = "movie_2")
            val item3 = testWatchlistItem.copy(id = "movie_3", ownerId = 2)

            repository.insert(item1)
            repository.insert(item2)
            repository.insert(item3)

            val items = repository.getAllItems(testWatchlistItem.ownerId)
            expectThat(items).hasSize(2)
        }

    @Test
    fun shouldObserveAllItemsByOwnerId() =
        runTest {
            database.userDao().insert(testUser.copy(id = 2))
            val item1 = testWatchlistItem.copy(id = "movie_1")
            val item2 = testWatchlistItem.copy(id = "movie_2")
            val item3 = testWatchlistItem.copy(id = "movie_3", ownerId = 2)

            repository.insert(item1)
            repository.insert(item2)
            repository.insert(item3)

            repository.getAllItemsInFlow(testWatchlistItem.ownerId).test {
                val result = awaitItem()
                expectThat(result).hasSize(2)
            }
        }

    @Test
    fun shouldRemoveWatchlistItem() =
        runTest {
            repository.insert(testWatchlistItem)

            repository.remove(testWatchlistItem)

            val retrievedItem = repository.getWatchlistItemById(
                testWatchlistItem.id,
                testWatchlistItem.ownerId,
            )
            expectThat(retrievedItem).isNull()
        }

    @Test
    fun shouldRemoveWatchlistItemById() =
        runTest {
            repository.insert(testWatchlistItem)

            repository.removeById(testWatchlistItem.id, testWatchlistItem.ownerId)

            val retrievedItem = repository.getWatchlistItemById(
                testWatchlistItem.id,
                testWatchlistItem.ownerId,
            )
            expectThat(retrievedItem).isNull()
        }

    @Test
    fun shouldRemoveAllWatchlistItemsForOwner() =
        runTest {
            database.userDao().insert(testUser.copy(id = 2))
            val item1 = testWatchlistItem.copy(id = "movie_1")
            val item2 = testWatchlistItem.copy(id = "movie_2")
            val item3 = testWatchlistItem.copy(id = "movie_3", ownerId = 2)

            repository.insert(item1)
            repository.insert(item2)
            repository.insert(item3)

            repository.removeAll(testWatchlistItem.ownerId)

            repository.getAllItemsInFlow(testWatchlistItem.ownerId).test {
                val result = awaitItem()
                expectThat(result).isEmpty()
            }

            // Verify other owner's data is not affected
            repository.getAllItemsInFlow(2).test {
                val result = awaitItem()
                expectThat(result).hasSize(1)
            }
        }

    @Test
    fun shouldReturnNullForNonExistentItem() =
        runTest {
            val retrievedItem = repository.getWatchlistItemById("non_existent", 1)
            expectThat(retrievedItem).isNull()
        }

    @Test
    fun shouldNotRemoveItemWithDifferentOwnerId() =
        runTest {
            repository.insert(testWatchlistItem)

            repository.removeById(testWatchlistItem.id, 999)

            val retrievedItem = repository.getWatchlistItemById(
                testWatchlistItem.id,
                testWatchlistItem.ownerId,
            )
            expectThat(retrievedItem).isNotNull()
        }
}
