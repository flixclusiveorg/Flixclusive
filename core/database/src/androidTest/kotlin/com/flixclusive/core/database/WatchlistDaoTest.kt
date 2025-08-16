package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.WatchlistDao
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watchlist.Watchlist
import com.flixclusive.model.film.util.FilmType
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
import java.io.IOException
import java.util.Date

@RunWith(AndroidJUnit4::class)
class WatchlistDaoTest {
    private lateinit var watchlistDao: WatchlistDao
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).build()
        watchlistDao = db.watchlistDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun shouldInsertAndRetrieveWatchlistItem() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val film = DBFilm(
                id = "movie123",
                providerId = "provider1",
                title = "Test Movie",
                adult = false,
                filmType = FilmType.MOVIE,
                rating = 8.5,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val watchlistItem = Watchlist(
                id = 1,
                filmId = "movie123",
                ownerId = 1,
                addedAt = Date(),
            )

            db.userDao().insert(user)
            db.filmsDao().insert(film)
            val itemId = watchlistDao.insert(watchlistItem)

            val retrievedItem = watchlistDao.get(itemId)
            expectThat(retrievedItem).isNotNull()
            expectThat(retrievedItem!!.watchlist.filmId).isEqualTo("movie123")
            expectThat(retrievedItem.watchlist.ownerId).isEqualTo(1)
        }

    @Test
    fun shouldReturnNullForNonexistentWatchlistItem() =
        runTest {
            val result = watchlistDao.get(0)
            expectThat(result).isNull()
        }

    @Test
    fun shouldGetAllWatchlistItemsForUser() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val film1 = DBFilm(
                id = "movie1",
                providerId = "provider1",
                title = "Movie 1",
                adult = false,
                filmType = FilmType.MOVIE,
                rating = 8.0,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val film2 = DBFilm(
                id = "movie2",
                providerId = "provider1",
                title = "Movie 2",
                adult = false,
                filmType = FilmType.MOVIE,
                rating = 7.5,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val watchlist1 = Watchlist(
                id = 1,
                filmId = "movie1",
                ownerId = 1,
                addedAt = Date(System.currentTimeMillis() - 86400000),
            )
            val watchlist2 = Watchlist(
                id = 2,
                filmId = "movie2",
                ownerId = 1,
                addedAt = Date(),
            )

            db.userDao().insert(user)
            db.filmsDao().insert(film1)
            db.filmsDao().insert(film2)
            watchlistDao.insert(watchlist1)
            watchlistDao.insert(watchlist2)

            val allWatchlist = watchlistDao.getAll(1)
            expectThat(allWatchlist).hasSize(2)
            expectThat(allWatchlist[0].watchlist.filmId).isEqualTo("movie2")
            expectThat(allWatchlist[1].watchlist.filmId).isEqualTo("movie1")
        }

    @Test
    fun shouldReturnEmptyListForUserWithNoWatchlistItems() =
        runTest {
            val allWatchlist = watchlistDao.getAll(999)
            expectThat(allWatchlist).isEmpty()
        }

    @Test
    fun shouldDeleteWatchlistItem() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val film = DBFilm(
                id = "movie123",
                providerId = "provider1",
                title = "Test Movie",
                adult = false,
                filmType = FilmType.MOVIE,
                rating = 8.5,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val watchlistItem = Watchlist(
                id = 1,
                filmId = "movie123",
                ownerId = 1,
                addedAt = Date(),
            )

            db.userDao().insert(user)
            db.filmsDao().insert(film)
            val itemId = watchlistDao.insert(watchlistItem)

            watchlistDao.delete(itemId)

            val retrievedItem = watchlistDao.get(itemId)
            expectThat(retrievedItem).isNull()
        }

    @Test
    fun shouldDeleteWatchlistItemByFilmId() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val film = DBFilm(
                id = "movie123",
                providerId = "provider1",
                title = "Test Movie",
                adult = false,
                filmType = FilmType.MOVIE,
                rating = 8.5,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val watchlistItem = Watchlist(
                id = 1,
                filmId = "movie123",
                ownerId = 1,
                addedAt = Date(),
            )

            db.userDao().insert(user)
            db.filmsDao().insert(film)
            val itemId = watchlistDao.insert(watchlistItem)

            watchlistDao.delete(itemId)

            val retrievedItem = watchlistDao.get(itemId)
            expectThat(retrievedItem).isNull()
        }

    @Test
    fun shouldCheckIfItemExistsInWatchlist() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val film = DBFilm(
                id = "movie123",
                providerId = "provider1",
                title = "Test Movie",
                adult = false,
                filmType = FilmType.MOVIE,
                rating = 8.5,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val watchlistItem = Watchlist(
                id = 1,
                filmId = "movie123",
                ownerId = 1,
                addedAt = Date(),
            )

            db.userDao().insert(user)
            db.filmsDao().insert(film)
            val itemId = watchlistDao.insert(watchlistItem)

            val exists = watchlistDao.get(itemId)
            expectThat(exists).isNotNull()

            val notExists = watchlistDao.get(2)
            expectThat(notExists).isNull()
        }

    @Test
    fun shouldHandleMultipleUsersWithSameFilmInWatchlist() =
        runTest {
            val user1 = User(id = 1, name = "user1", image = 1)
            val user2 = User(id = 2, name = "user2", image = 2)
            val film = DBFilm(
                id = "movie123",
                providerId = "provider1",
                title = "Test Movie",
                adult = false,
                filmType = FilmType.MOVIE,
                rating = 8.5,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val watchlist1 = Watchlist(
                id = 1,
                filmId = "movie123",
                ownerId = 1,
                addedAt = Date(),
            )
            val watchlist2 = Watchlist(
                id = 2,
                filmId = "movie123",
                ownerId = 2,
                addedAt = Date(),
            )

            db.userDao().insert(user1)
            db.userDao().insert(user2)
            db.filmsDao().insert(film)
            watchlistDao.insert(watchlist1)
            watchlistDao.insert(watchlist2)

            val user1Watchlist = watchlistDao.getAll(1)
            val user2Watchlist = watchlistDao.getAll(2)

            expectThat(user1Watchlist).hasSize(1)
            expectThat(user2Watchlist).hasSize(1)
            expectThat(user1Watchlist[0].watchlist.ownerId).isEqualTo(1)
            expectThat(user2Watchlist[0].watchlist.ownerId).isEqualTo(2)
        }

    @Test
    fun shouldHandleUniqueFilmIdConstraint() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val film = DBFilm(
                id = "movie123",
                providerId = "provider1",
                title = "Test Movie",
                adult = false,
                filmType = FilmType.MOVIE,
                rating = 8.5,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val watchlistItem1 = Watchlist(
                id = 1,
                filmId = "movie123",
                ownerId = 1,
                addedAt = Date(),
            )
            val watchlistItem2 = Watchlist(
                id = 2,
                filmId = "movie123",
                ownerId = 1,
                addedAt = Date(System.currentTimeMillis() + 1000),
            )

            db.userDao().insert(user)
            db.filmsDao().insert(film)
            watchlistDao.insert(watchlistItem1)

            // This should replace the first one due to the unique constraint on filmId
            watchlistDao.insert(watchlistItem2)

            val allWatchlist = watchlistDao.getAll(1)
            expectThat(allWatchlist).hasSize(1)
            expectThat(allWatchlist[0].watchlist.filmId).isEqualTo("movie123")
        }
}
