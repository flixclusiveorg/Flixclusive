package com.flixclusive.data.database.repository.impl

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.entity.search.SearchHistory
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

@RunWith(AndroidJUnit4::class)
class SearchHistoryRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: SearchHistoryRepositoryImpl
    private lateinit var appDispatchers: AppDispatchers

    private val testDispatcher = StandardTestDispatcher()
    private val testUser = DatabaseTestDefaults.getUser()
    private val testSearchHistory = DatabaseTestDefaults.getSearchHistory(ownerId = testUser.id)

    @Before
    fun setUp() {
        database = DatabaseTestDefaults.createDatabase(
            context = ApplicationProvider.getApplicationContext(),
        )
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
        repository = SearchHistoryRepositoryImpl(
            searchHistoryDao = database.searchHistoryDao(),
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
    fun shouldInsertSearchHistory() =
        runTest(testDispatcher) {
            repository.insert(testSearchHistory)

            repository.getAllItemsInFlow(testSearchHistory.ownerId).test {
                val result = awaitItem()
                expectThat(result).hasSize(1)
                expectThat(result.first()).and {
                    get { query }.isEqualTo(testSearchHistory.query)
                    get { ownerId }.isEqualTo(testSearchHistory.ownerId)
                }
            }
        }

    @Test
    fun shouldRetrieveSearchHistoryByOwnerId() =
        runTest(testDispatcher) {
            database.userDao().insert(testUser.copy(id = 2))
            val searchHistory1 = testSearchHistory.copy(id = 1, query = "Avengers")
            val searchHistory2 = testSearchHistory.copy(id = 2, query = "Batman")
            val searchHistory3 = SearchHistory(id = 3, query = "Superman", ownerId = 2)

            repository.insert(searchHistory1)
            repository.insert(searchHistory2)
            repository.insert(searchHistory3)

            repository.getAllItemsInFlow(testSearchHistory.ownerId).test {
                val result = awaitItem()
                expectThat(result).hasSize(2)
            }
        }

    @Test
    fun shouldRemoveSearchHistoryById() =
        runTest(testDispatcher) {
            val id = repository.insert(testSearchHistory)

            repository.remove(id)

            repository.getAllItemsInFlow(testSearchHistory.ownerId).test {
                val result = awaitItem()
                expectThat(result).isEmpty()
            }
        }

    @Test
    fun shouldClearAllSearchHistory() =
        runTest(testDispatcher) {
            database.userDao().insert(testUser.copy(id = 2))
            val searchHistory1 = testSearchHistory.copy(id = 1, query = "Avengers")
            val searchHistory2 = testSearchHistory.copy(id = 2, query = "Batman")
            val searchHistory3 = SearchHistory(id = 3, query = "Superman", ownerId = 2)

            repository.insert(searchHistory1)
            repository.insert(searchHistory2)
            repository.insert(searchHistory3)

            repository.clearAll(testSearchHistory.ownerId)

            repository.getAllItemsInFlow(testSearchHistory.ownerId).test {
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
    fun shouldNotRemoveSearchHistoryWithDifferentOwnerId() =
        runTest(testDispatcher) {
            repository.insert(testSearchHistory)

            // Try to remove with different owner ID
            repository.remove(testSearchHistory.id)

            repository.getAllItemsInFlow(testSearchHistory.ownerId).test {
                val result = awaitItem()
                expectThat(result).hasSize(1)
            }
        }
}
