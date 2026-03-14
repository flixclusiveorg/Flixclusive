package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.films.DBFilmDao
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.model.film.util.FilmType
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
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DBFilmDaoTest {
    private lateinit var dbFilmDao: DBFilmDao
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).build()
        dbFilmDao = db.filmsDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun shouldInsertAndRetrieveFilm() =
        runTest {
            val film = DBFilm(
                id = "movie123",
                providerId = "provider1",
                imdbId = "tt1234567",
                tmdbId = 12345,
                title = "Test Movie",
                posterImage = "https://example.com/poster.jpg",
                filmType = FilmType.MOVIE
            )

            dbFilmDao.insert(film)

            val retrievedFilm = dbFilmDao.get("movie123")
            expectThat(retrievedFilm).isNotNull().and {
                get { id }.isEqualTo("movie123")
                get { title }.isEqualTo("Test Movie")
                get { filmType }.isEqualTo(FilmType.MOVIE)
                get { imdbId }.isEqualTo("tt1234567")
                get { tmdbId }.isEqualTo(12345)
            }
        }

    @Test
    fun shouldReturnNullForNonexistentFilm() =
        runTest {
            val result = dbFilmDao.get("nonexistent")
            expectThat(result).isNull()
        }

    @Test
    fun shouldInsertAndRetrieveTvSeries() =
        runTest {
            val tvSeries = DBFilm(
                id = "series123",
                providerId = "provider1",
                title = "Test Series",
                filmType = FilmType.TV_SHOW
            )

            dbFilmDao.insert(tvSeries)

            val retrievedSeries = dbFilmDao.get("series123")
            expectThat(retrievedSeries).isNotNull().and {
                get { filmType }.isEqualTo(FilmType.TV_SHOW)
                get { posterImage }.isNull()
                get { imdbId }.isNull()
            }
        }

    @Test
    fun shouldUpdateFilm() =
        runTest {
            val film = DBFilm(
                id = "movie123",
                providerId = "provider1",
                title = "Original Title",
            )

            dbFilmDao.insert(film)

            val updatedFilm = film.copy(
                title = "Updated Title",
                updatedAt = System.currentTimeMillis() + 1000,
            )
            dbFilmDao.insert(updatedFilm)

            val retrievedFilm = dbFilmDao.get("movie123")
            expectThat(retrievedFilm).isNotNull().and {
                get { title }.isEqualTo("Updated Title")
            }
        }

    @Test
    fun shouldDeleteFilm() =
        runTest {
            val film = DBFilm(
                id = "movie123",
                providerId = "provider1",
                title = "Test Movie",
            )

            dbFilmDao.insert(film)
            dbFilmDao.delete("movie123")

            val retrievedFilm = dbFilmDao.get("movie123")
            expectThat(retrievedFilm).isNull()
        }

    @Test
    fun shouldDeleteAllFilms() =
        runTest {
            dbFilmDao.insert(DBFilm(id = "movie1", providerId = "p1", title = "Movie 1"))
            dbFilmDao.insert(DBFilm(id = "movie2", providerId = "p1", title = "Movie 2"))
            dbFilmDao.deleteAll()

            expectThat(dbFilmDao.get("movie1")).isNull()
            expectThat(dbFilmDao.get("movie2")).isNull()
        }

    @Test
    fun shouldGetAllAsFlow() =
        runTest {
            dbFilmDao.insert(DBFilm(id = "movie1", providerId = "p1", title = "Movie 1"))
            dbFilmDao.insert(DBFilm(id = "movie2", providerId = "p1", title = "Movie 2"))

            val allFilms = dbFilmDao.getAllAsFlow().first()
            expectThat(allFilms).hasSize(2)
        }

    @Test
    fun shouldReturnEmptyFlowWhenNoFilms() =
        runTest {
            val allFilms = dbFilmDao.getAllAsFlow().first()
            expectThat(allFilms).isEmpty()
        }

    @Test
    fun shouldHandleFilmWithNullOptionalFields() =
        runTest {
            val film = DBFilm(
                id = "minimal123",
                providerId = "provider1",
                title = "Minimal Movie",
            )

            dbFilmDao.insert(film)

            val retrievedFilm = dbFilmDao.get("minimal123")
            expectThat(retrievedFilm).isNotNull().and {
                get { posterImage }.isNull()
                get { imdbId }.isNull()
                get { tmdbId }.isNull()
            }
        }
}
