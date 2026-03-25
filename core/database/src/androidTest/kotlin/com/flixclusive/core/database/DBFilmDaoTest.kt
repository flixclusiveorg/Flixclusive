package com.flixclusive.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.films.DBFilmDao
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.film.DBFilmExternalId
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
import java.sql.Date

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
    fun shouldUpsertFilmAndRetrieveFilm() =
        runTest {
            val film = DBFilm(
                id = "movie123",
                providerId = "provider1",
                title = "Test Movie",
                posterImage = "https://example.com/poster.jpg",
                filmType = FilmType.MOVIE
            )

            dbFilmDao.upsertFilm(film)

            val retrievedFilm = dbFilmDao.get("movie123")
            expectThat(retrievedFilm).isNotNull().and {
                get { id }.isEqualTo("movie123")
                get { title }.isEqualTo("Test Movie")
                get { filmType }.isEqualTo(FilmType.MOVIE)
            }
        }

    @Test
    fun shouldReturnNullForNonexistentFilm() =
        runTest {
            val result = dbFilmDao.get("nonexistent")
            expectThat(result).isNull()
        }

    @Test
    fun shouldUpsertFilmAndRetrieveTvSeries() =
        runTest {
            val tvSeries = DBFilm(
                id = "series123",
                providerId = "provider1",
                title = "Test Series",
                filmType = FilmType.TV_SHOW
            )

            dbFilmDao.upsertFilm(tvSeries)

            val retrievedSeries = dbFilmDao.get("series123")
            expectThat(retrievedSeries).isNotNull().and {
                get { filmType }.isEqualTo(FilmType.TV_SHOW)
                get { posterImage }.isNull()
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

            dbFilmDao.upsertFilm(film)

            val updatedFilm = film.copy(
                title = "Updated Title",
                updatedAt = Date(System.currentTimeMillis() + 1000),
            )
            dbFilmDao.upsertFilm(updatedFilm)

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

            dbFilmDao.upsertFilm(film)
            dbFilmDao.delete("movie123")

            val retrievedFilm = dbFilmDao.get("movie123")
            expectThat(retrievedFilm).isNull()
        }

    @Test
    fun shouldDeleteAllFilms() =
        runTest {
            dbFilmDao.upsertFilm(DBFilm(id = "movie1", providerId = "p1", title = "Movie 1"))
            dbFilmDao.upsertFilm(DBFilm(id = "movie2", providerId = "p1", title = "Movie 2"))
            dbFilmDao.deleteAll()

            expectThat(dbFilmDao.get("movie1")).isNull()
            expectThat(dbFilmDao.get("movie2")).isNull()
        }

    @Test
    fun shouldGetAllAsFlow() =
        runTest {
            dbFilmDao.upsertFilm(DBFilm(id = "movie1", providerId = "p1", title = "Movie 1"))
            dbFilmDao.upsertFilm(DBFilm(id = "movie2", providerId = "p1", title = "Movie 2"))

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

            dbFilmDao.upsertFilm(film)

            val retrievedFilm = dbFilmDao.get("minimal123")
            expectThat(retrievedFilm).isNotNull().and {
                get { posterImage }.isNull()
            }
        }

    @Test
    fun upsertIds_allowsDifferentProvider_sameFilmAndExternal() = runTest {
        val dao = db.filmsDao()
        val film = DBFilm(
            id = "film_1",
            providerId = "provider_a",
            title = "Test Film",
        )
        dao.upsertFilm(film)

        val base = DBFilmExternalId("film_1", "provider_a", "api", "ext_1")

        dao.upsertIds(listOf(base))
        dao.upsertIds(listOf(base.copy(providerId = "provider_b")))

        val result = dao.get("film_1")!!

        expectThat(result.externalIds).hasSize(2)
    }

    @Test
    fun upsertIds_allowsDifferentExternal_sameFilmAndProvider() = runTest {
        val dao = db.filmsDao()
        val film = DBFilm(
            id = "film_1",
            providerId = "provider_a",
            title = "Test Film",
        )
        dao.upsertFilm(film)

        val base = DBFilmExternalId("film_1", "provider_a", "api", "ext_1")

        dao.upsertIds(listOf(base))
        dao.upsertIds(listOf(base.copy(externalId = "ext_2")))

        val result = dao.get("film_1")!!

        expectThat(result.externalIds).hasSize(2)
    }

    @Test
    fun upsertIds_allowsDifferentFilm_sameProviderAndExternal() = runTest {
        val dao = db.filmsDao()
        val film1 = DBFilm(
            id = "film_1",
            providerId = "provider_a",
            title = "Test Film",
        )
        val film2 = DBFilm(
            id = "film_2",
            providerId = "provider_a",
            title = "Test Film 2",
        )

        dao.upsertFilm(film1)
        dao.upsertFilm(film2)

        val base = DBFilmExternalId("film_1", "provider_a", "api", "ext_1")

        dao.upsertIds(listOf(base))
        dao.upsertIds(listOf(base.copy(filmId = "film_2")))

        val result1 = dao.get("film_1")!!
        val result2 = dao.get("film_2")!!

        expectThat(result1.externalIds).hasSize(1)
        expectThat(result2.externalIds).hasSize(1)
    }

    @Test
    fun upsertIds_sameCompositeKey_updatesInsteadOfDuplicating() = runTest {
        val dao = db.filmsDao()
        val film = DBFilm(
            id = "film_1",
            providerId = "provider_a",
            title = "Test Film",
        )
        dao.upsertFilm(film)

        val base = DBFilmExternalId("film_1", "provider_a", "api", "ext_1")

        dao.upsertIds(listOf(base))

        val updated = base.copy(source = "updated_source")
        dao.upsertIds(listOf(updated))

        val result = dao.get("film_1")!!

        expectThat(result.externalIds).hasSize(1)
        expectThat(result.externalIds.first().source).isEqualTo("updated_source")
    }

    @Test
    fun upsertIds_multipleMixedEntries_correctFinalState() = runTest {
        val dao = db.filmsDao()
        val film = DBFilm(
            id = "film_1",
            providerId = "provider_a",
            title = "Test Film",
        )
        dao.upsertFilm(film)

        val items = listOf(
            DBFilmExternalId("film_1", "provider_a", "api", "ext_1"),
            DBFilmExternalId("film_1", "provider_a", "api", "ext_2"),
            DBFilmExternalId("film_1", "provider_b", "api", "ext_1"),
        )

        dao.upsertIds(items)

        dao.upsertIds(
            listOf(
                DBFilmExternalId("film_1", "provider_a", "updated", "ext_1")
            )
        )

        val result = dao.get("film_1")!!

        expectThat(result.externalIds).hasSize(3)

        val updated = result.externalIds.find {
            it.providerId == "provider_a" && it.externalId == "ext_1"
        }

        expectThat(updated).isNotNull()
        expectThat(updated!!.source).isEqualTo("updated")
    }

    @Test
    fun deleteFilm_cascadesToExternalIds() = runTest {
        val dao = db.filmsDao()
        val film = DBFilm(
            id = "film_1",
            providerId = "provider_a",
            title = "Test Film",
        )
        dao.upsertFilm(film)

        val ids = listOf(
            DBFilmExternalId("film_1", "provider_a", "api", "ext_1"),
            DBFilmExternalId("film_1", "provider_b", "api", "ext_2"),
        )

        dao.upsertIds(ids)

        dao.delete("film_1")

        val result = dao.get("film_1")

        expectThat(result).isNull()
    }

    @Test
    fun deleteAll_removesEverything() = runTest {
        val dao = db.filmsDao()
        val film = DBFilm(
            id = "film_1",
            providerId = "provider_a",
            title = "Test Film",
        )
        dao.upsertFilm(film)

        dao.upsertIds(
            listOf(
                DBFilmExternalId("film_1", "provider_a", "api", "ext_1")
            )
        )

        dao.deleteAll()

        val result = dao.get("film_1")

        expectThat(result).isNull()
    }
}
