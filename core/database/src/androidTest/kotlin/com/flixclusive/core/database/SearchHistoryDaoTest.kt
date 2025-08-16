package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.SearchHistoryDao
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.database.entity.user.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import java.io.IOException
import java.util.Date

@RunWith(AndroidJUnit4::class)
class SearchHistoryDaoTest {
    private lateinit var searchHistoryDao: SearchHistoryDao
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).build()
        searchHistoryDao = db.searchHistoryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun shouldInsertAndRetrieveSearchHistory() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val searchHistory = SearchHistory(
                id = 1,
                query = "action movies",
                ownerId = 1,
                searchedOn = Date(),
            )

            db.userDao().insert(user)
            searchHistoryDao.insert(searchHistory)

            val userHistory = searchHistoryDao.getAll(1).first()
            expectThat(userHistory).hasSize(1)
            expectThat(userHistory[0].query).isEqualTo("action movies")
            expectThat(userHistory[0].ownerId).isEqualTo(1)
        }

    @Test
    fun shouldGetAllSearchHistoryForUser() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val searchHistory1 = SearchHistory(
                id = 1,
                query = "action movies",
                ownerId = 1,
                searchedOn = Date(System.currentTimeMillis() - 86400000),
            )
            val searchHistory2 = SearchHistory(
                id = 2,
                query = "comedy series",
                ownerId = 1,
                searchedOn = Date(),
            )
            val searchHistory3 = SearchHistory(
                id = 3,
                query = "horror films",
                ownerId = 2,
                searchedOn = Date(),
            )

            db.userDao().insert(user)
            db.userDao().insert(User(id = 2, name = "otheruser", image = 2))
            searchHistoryDao.insert(searchHistory1)
            searchHistoryDao.insert(searchHistory2)
            searchHistoryDao.insert(searchHistory3)

            val userHistory = searchHistoryDao.getAll(1).first()
            expectThat(userHistory).hasSize(2)
            expectThat(userHistory.map { it.query }).isEqualTo(listOf("comedy series", "action movies"))
        }

    @Test
    fun shouldReturnEmptyListForUserWithNoSearchHistory() =
        runTest {
            val userHistory = searchHistoryDao.getAll(999).first()
            expectThat(userHistory).isEmpty()
        }

    @Test
    fun shouldUpdateSearchHistory() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val searchHistory = SearchHistory(
                id = 1,
                query = "action movies",
                ownerId = 1,
                searchedOn = Date(),
            )

            db.userDao().insert(user)
            searchHistoryDao.insert(searchHistory)

            val updatedHistory = searchHistory.copy(
                query = "updated search query",
                searchedOn = Date(),
            )
            searchHistoryDao.insert(updatedHistory)

            val userHistory = searchHistoryDao.getAll(1).first()
            expectThat(userHistory).hasSize(1)
            expectThat(userHistory[0].query).isEqualTo("updated search query")
        }

    @Test
    fun shouldDeleteSearchHistory() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val searchHistory = SearchHistory(
                id = 1,
                query = "action movies",
                ownerId = 1,
                searchedOn = Date(),
            )

            db.userDao().insert(user)
            val itemId = searchHistoryDao.insert(searchHistory)

            searchHistoryDao.delete(itemId.toInt())

            val userHistory = searchHistoryDao.getAll(1).first()
            expectThat(userHistory).isEmpty()
        }

    @Test
    fun shouldDeleteAllSearchHistoryForUser() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val searchHistory1 = SearchHistory(
                id = 1,
                query = "action movies",
                ownerId = 1,
                searchedOn = Date(),
            )
            val searchHistory2 = SearchHistory(
                id = 2,
                query = "comedy series",
                ownerId = 1,
                searchedOn = Date(),
            )

            db.userDao().insert(user)
            searchHistoryDao.insert(searchHistory1)
            searchHistoryDao.insert(searchHistory2)

            searchHistoryDao.deleteAll(1)

            val userHistory = searchHistoryDao.getAll(1).first()
            expectThat(userHistory).isEmpty()
        }

    @Test
    fun shouldHandleMaximumSearchHistoryLimit() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            db.userDao().insert(user)

            val searchQueries = (1..25).map { i ->
                SearchHistory(
                    id = i,
                    query = "search query $i",
                    ownerId = 1,
                    searchedOn = Date(System.currentTimeMillis() + i * 1000L),
                )
            }

            searchQueries.forEach { searchHistoryDao.insert(it) }

            val allHistory = searchHistoryDao.getAll(1).first()
            expectThat(allHistory).hasSize(25)

            // Verify they're ordered by most recent first
            expectThat(allHistory.first().query).isEqualTo("search query 25")
            expectThat(allHistory.last().query).isEqualTo("search query 1")
        }

    @Test
    fun shouldHandleUniqueConstraint() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val searchHistory1 = SearchHistory(
                id = 1,
                query = "duplicate query",
                ownerId = 1,
                searchedOn = Date(),
            )
            val searchHistory2 = SearchHistory(
                id = 2,
                query = "duplicate query",
                ownerId = 1,
                searchedOn = Date(System.currentTimeMillis() + 1000),
            )

            db.userDao().insert(user)
            searchHistoryDao.insert(searchHistory1)
            // This should replace the first one due to the unique constraint
            searchHistoryDao.insert(searchHistory2)

            val userHistory = searchHistoryDao.getAll(1).first()
            expectThat(userHistory).hasSize(1)
            expectThat(userHistory[0].query).isEqualTo("duplicate query")
        }
}
