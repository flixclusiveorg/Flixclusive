package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.DBFilmDao
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.model.film.util.FilmType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.io.IOException
import java.util.Date

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
                language = "en",
                adult = false,
                title = "Test Movie",
                runtime = 120,
                backdropImage = "https://example.com/backdrop.jpg",
                posterImage = "https://example.com/poster.jpg",
                overview = "A thrilling test movie",
                homePage = "https://example.com",
                releaseDate = "2023-01-01",
                logoImage = "https://example.com/logo.jpg",
                year = 2023,
                filmType = FilmType.MOVIE,
                rating = 8.5,
                customProperties = mapOf("genre" to "Action"),
                createdAt = Date(),
                updatedAt = Date(),
            )

            dbFilmDao.insert(film)

            val retrievedFilm = dbFilmDao.get("movie123")
            expectThat(retrievedFilm).isNotNull()
            expectThat(retrievedFilm!!.id).isEqualTo("movie123")
            expectThat(retrievedFilm.title).isEqualTo("Test Movie")
            expectThat(retrievedFilm.rating).isEqualTo(8.5)
            expectThat(retrievedFilm.filmType).isEqualTo(FilmType.MOVIE)
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
                imdbId = null,
                tmdbId = null,
                language = "en",
                adult = false,
                title = "Test Series",
                runtime = null,
                backdropImage = null,
                posterImage = null,
                overview = "A great test series",
                homePage = null,
                releaseDate = "2023-01-01",
                logoImage = null,
                year = 2023,
                filmType = FilmType.TV_SHOW,
                rating = 9.0,
                customProperties = mapOf("seasons" to "3"),
                createdAt = Date(),
                updatedAt = Date(),
            )

            dbFilmDao.insert(tvSeries)

            val retrievedSeries = dbFilmDao.get("series123")
            expectThat(retrievedSeries).isNotNull()
            expectThat(retrievedSeries!!.filmType).isEqualTo(FilmType.TV_SHOW)
            expectThat(retrievedSeries.runtime).isNull()
            expectThat(retrievedSeries.customProperties["seasons"]).isEqualTo("3")
        }

    @Test
    fun shouldUpdateFilm() =
        runTest {
            val film = DBFilm(
                id = "movie123",
                providerId = "provider1",
                title = "Original Title",
                adult = false,
                filmType = FilmType.MOVIE,
                rating = 7.0,
                overview = "Original description",
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )

            dbFilmDao.insert(film)

            val updatedFilm = film.copy(
                title = "Updated Title",
                rating = 8.5,
                overview = "Updated description",
                updatedAt = Date(),
            )
            dbFilmDao.insert(updatedFilm)

            val retrievedFilm = dbFilmDao.get("movie123")
            expectThat(retrievedFilm).isNotNull()
            expectThat(retrievedFilm!!.title).isEqualTo("Updated Title")
            expectThat(retrievedFilm.rating).isEqualTo(8.5)
            expectThat(retrievedFilm.overview).isEqualTo("Updated description")
        }

    @Test
    fun shouldDeleteFilm() =
        runTest {
            val film = DBFilm(
                id = "movie123",
                providerId = "provider1",
                title = "Test Movie",
                adult = false,
                filmType = FilmType.MOVIE,
                rating = 8.0,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )

            dbFilmDao.insert(film)
            dbFilmDao.delete("movie123")

            val retrievedFilm = dbFilmDao.get("movie123")
            expectThat(retrievedFilm).isNull()
        }

    @Test
    fun shouldDeleteAllFilms() =
        runTest {
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

            dbFilmDao.insert(film1)
            dbFilmDao.insert(film2)
            dbFilmDao.deleteAll()

            val allFilms = dbFilmDao.get(film1.id)
            expectThat(allFilms).isNull()
        }

    @Test
    fun shouldHandleFilmWithNullOptionalFields() =
        runTest {
            val film = DBFilm(
                id = "minimal123",
                providerId = "provider1",
                title = "Minimal Movie",
                adult = false,
                filmType = FilmType.MOVIE,
                rating = 6.0,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )

            dbFilmDao.insert(film)

            val retrievedFilm = dbFilmDao.get("minimal123")
            expectThat(retrievedFilm).isNotNull()
            expectThat(retrievedFilm!!.posterImage).isNull()
            expectThat(retrievedFilm.backdropImage).isNull()
            expectThat(retrievedFilm.logoImage).isNull()
            expectThat(retrievedFilm.imdbId).isNull()
        }

    @Test
    fun shouldHandleFilmWithComplexCustomProperties() =
        runTest {
            val customProps = mapOf(
                "genres" to "Action,Adventure,Comedy",
                "duration" to "180",
                "recommendations" to "rec1,rec2,rec3",
                "similar" to "sim1,sim2,sim3",
            )

            val film = DBFilm(
                id = "complex123",
                providerId = "provider1",
                title = "Complex Movie",
                adult = false,
                filmType = FilmType.MOVIE,
                rating = 9.2,
                overview = "A complex movie with many properties",
                customProperties = customProps,
                createdAt = Date(),
                updatedAt = Date(),
            )

            dbFilmDao.insert(film)

            val retrievedFilm = dbFilmDao.get("complex123")
            expectThat(retrievedFilm).isNotNull()
            expectThat(retrievedFilm!!.customProperties["genres"]).isEqualTo("Action,Adventure,Comedy")
            expectThat(retrievedFilm.customProperties["duration"]).isEqualTo("180")
            expectThat(retrievedFilm.customProperties.size).isEqualTo(4)
        }

    @Test
    fun shouldHandleAdultContent() =
        runTest {
            val adultFilm = DBFilm(
                id = "adult123",
                providerId = "provider1",
                title = "Adult Movie",
                adult = true,
                filmType = FilmType.MOVIE,
                rating = 7.0,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )

            dbFilmDao.insert(adultFilm)

            val retrievedFilm = dbFilmDao.get("adult123")
            expectThat(retrievedFilm).isNotNull()
            expectThat(retrievedFilm!!.adult).isEqualTo(true)
        }
}
