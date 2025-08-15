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
class WatchHistoryRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: WatchHistoryRepositoryImpl

    private val testWatchHistoryItem = DatabaseTestDefaults.getWatchHistoryItem()
    private val testUser = DatabaseTestDefaults.getUser()

    @Before
    fun setUp() {
        database = DatabaseTestDefaults.createDatabase(
            context = ApplicationProvider.getApplicationContext(),
        )
        repository = WatchHistoryRepositoryImpl(database.watchHistoryDao())

        runBlocking {
            database.userDao().insert(testUser)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun shouldInsertWatchHistoryItem() =
        runTest {
            repository.insert(testWatchHistoryItem)

            repository.getAllItemsInFlow(testWatchHistoryItem.ownerId).test {
                val result = awaitItem()
                expectThat(result).hasSize(1)
                expectThat(result.first()).and {
                    get { id }.isEqualTo(testWatchHistoryItem.id)
                    get { film.title }.isEqualTo(testWatchHistoryItem.film.title)
                    get { ownerId }.isEqualTo(testWatchHistoryItem.ownerId)
                }
            }
        }

    @Test
    fun shouldRetrieveWatchHistoryItemById() =
        runTest {
            repository.insert(testWatchHistoryItem)

            val retrievedItem = repository.getWatchHistoryItemById(
                testWatchHistoryItem.id,
                testWatchHistoryItem.ownerId,
            )

            expectThat(retrievedItem).isNotNull().and {
                get { id }.isEqualTo(testWatchHistoryItem.id)
                get { film.title }.isEqualTo(testWatchHistoryItem.film.title)
                get { dateWatched }.isEqualTo(testWatchHistoryItem.dateWatched)
            }
        }

    @Test
    fun shouldObserveWatchHistoryItemById() =
        runTest {
            repository.insert(testWatchHistoryItem)

            repository
                .getWatchHistoryItemByIdInFlow(
                    testWatchHistoryItem.id,
                    testWatchHistoryItem.ownerId,
                ).test {
                    val result = awaitItem()
                    expectThat(result).isNotNull().and {
                        get { id }.isEqualTo(testWatchHistoryItem.id)
                        get { film.title }.isEqualTo(testWatchHistoryItem.film.title)
                    }
                }
        }

    @Test
    fun shouldRetrieveAllItemsByOwnerId() =
        runTest {
            val item1 = testWatchHistoryItem.copy(id = "movie_1")
            val item2 = testWatchHistoryItem.copy(id = "movie_2")
            val item3 = testWatchHistoryItem.copy(id = "movie_3", ownerId = 2)

            repository.insert(item1)
            repository.insert(item2)
            repository.insert(item3)

            repository.getAllItemsInFlow(testWatchHistoryItem.ownerId).test {
                val result = awaitItem()
                expectThat(result).hasSize(2)
            }
        }

    @Test
    fun shouldGetRandomWatchHistoryItems() =
        runTest {
            val item1 = testWatchHistoryItem.copy(id = "movie_1")
            val item2 = testWatchHistoryItem.copy(id = "movie_2")
            val item3 = testWatchHistoryItem.copy(id = "movie_3")

            repository.insert(item1)
            repository.insert(item2)
            repository.insert(item3)

            repository.getRandomWatchHistoryItems(testWatchHistoryItem.ownerId, 2).test {
                expectThat(awaitItem()).hasSize(2)
            }
        }

    @Test
    fun shouldDeleteWatchHistoryItemById() =
        runTest {
            repository.insert(testWatchHistoryItem)

            repository.deleteById(testWatchHistoryItem.id, testWatchHistoryItem.ownerId)

            val retrievedItem = repository.getWatchHistoryItemById(
                testWatchHistoryItem.id,
                testWatchHistoryItem.ownerId,
            )
            expectThat(retrievedItem).isNull()
        }

    @Test
    fun shouldRemoveAllWatchHistoryForOwner() =
        runTest {
            database.userDao().insert(testUser.copy(id = 2))
            val item1 = testWatchHistoryItem.copy(id = "movie_1")
            val item2 = testWatchHistoryItem.copy(id = "movie_2")
            val item3 = testWatchHistoryItem.copy(id = "movie_3", ownerId = 2)

            repository.insert(item1)
            repository.insert(item2)
            repository.insert(item3)

            repository.removeAll(testWatchHistoryItem.ownerId)

            repository.getAllItemsInFlow(testWatchHistoryItem.ownerId).test {
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
            val retrievedItem = repository.getWatchHistoryItemById("non_existent", 1)
            expectThat(retrievedItem).isNull()
        }

    @Test
    fun shouldNotDeleteItemWithDifferentOwnerId() =
        runTest {
            repository.insert(testWatchHistoryItem)

            repository.deleteById(testWatchHistoryItem.id, 999)

            val retrievedItem = repository.getWatchHistoryItemById(
                testWatchHistoryItem.id,
                testWatchHistoryItem.ownerId,
            )
            expectThat(retrievedItem).isNotNull()
        }
}
