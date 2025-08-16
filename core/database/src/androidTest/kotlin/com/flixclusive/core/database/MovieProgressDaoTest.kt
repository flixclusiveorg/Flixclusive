package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.MovieProgressDao
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
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
import java.util.Date

@RunWith(AndroidJUnit4::class)
class MovieProgressDaoTest {
    private lateinit var movieProgressDao: MovieProgressDao
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).build()
        movieProgressDao = db.movieProgressDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun shouldInsertAndRetrieveMovieProgress() =
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
            val movieProgress = MovieProgress(
                id = 1,
                filmId = "movie123",
                ownerId = 1,
                progress = 1800000L,
                status = WatchStatus.WATCHING,
                duration = 7200000L,
                watchedAt = Date(),
                watchCount = 1,
            )

            db.userDao().insert(user)
            db.filmsDao().insert(film)
            movieProgressDao.insert(movieProgress)

            val retrievedProgress = movieProgressDao.get(movieProgress.id)
            expectThat(retrievedProgress).isNotNull()
            expectThat(retrievedProgress!!.watchData.filmId).isEqualTo("movie123")
            expectThat(retrievedProgress.watchData.ownerId).isEqualTo(1)
            expectThat(retrievedProgress.watchData.progress).isEqualTo(1800000L)
        }

    @Test
    fun shouldReturnNullForNonexistentMovieProgress() =
        runTest {
            val result = movieProgressDao.get(999)
            expectThat(result).isNull()
        }

    @Test
    fun shouldGetAllMovieProgressForUser() =
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
            val progress1 = MovieProgress(
                id = 1,
                filmId = "movie1",
                ownerId = 1,
                progress = 1800000L,
                status = WatchStatus.WATCHING,
                duration = 7200000L,
                watchedAt = Date(System.currentTimeMillis() - 86400000),
                watchCount = 1,
            )
            val progress2 = MovieProgress(
                id = 2,
                filmId = "movie2",
                ownerId = 1,
                progress = 5400000L,
                status = WatchStatus.COMPLETED,
                duration = 9000000L,
                watchedAt = Date(),
                watchCount = 1,
            )

            db.userDao().insert(user)
            db.filmsDao().insert(film1)
            db.filmsDao().insert(film2)
            movieProgressDao.insert(progress1)
            movieProgressDao.insert(progress2)

            val allProgress = movieProgressDao.getAllAsFlow(1).first()
            expectThat(allProgress).hasSize(2)
            expectThat(allProgress[0].watchData.filmId).isEqualTo("movie2")
            expectThat(allProgress[1].watchData.filmId).isEqualTo("movie1")
        }

    @Test
    fun shouldReturnEmptyListForUserWithNoProgress() =
        runTest {
            val allProgress = movieProgressDao.getAllAsFlow(999).first()
            expectThat(allProgress).isEmpty()
        }

    @Test
    fun shouldGetRandomMovieProgress() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val films = (1..5).map { i ->
                DBFilm(
                    id = "movie$i",
                    providerId = "provider1",
                    title = "Movie $i",
                    adult = false,
                    filmType = FilmType.MOVIE,
                    rating = 8.0,
                    customProperties = emptyMap(),
                    createdAt = Date(),
                    updatedAt = Date(),
                )
            }
            val progressList = (1..5).map { i ->
                MovieProgress(
                    id = i.toLong(),
                    filmId = "movie$i",
                    ownerId = 1,
                    progress = 1800000L,
                    status = WatchStatus.WATCHING,
                    duration = 7200000L,
                    watchedAt = Date(),
                    watchCount = 1,
                )
            }

            db.userDao().insert(user)
            films.forEach { db.filmsDao().insert(it) }
            progressList.forEach { movieProgressDao.insert(it) }

            val randomProgress = movieProgressDao.getRandoms(1, 3).first()
            expectThat(randomProgress).hasSize(3)
        }

    @Test
    fun shouldUpdateMovieProgress() =
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
            val movieProgress = MovieProgress(
                id = 1,
                filmId = "movie123",
                ownerId = 1,
                progress = 1800000L,
                status = WatchStatus.WATCHING,
                duration = 7200000L,
                watchedAt = Date(),
                watchCount = 1,
            )

            db.userDao().insert(user)
            db.filmsDao().insert(film)
            movieProgressDao.insert(movieProgress)

            val updatedProgress = movieProgress.copy(
                progress = 3600000L,
                status = WatchStatus.COMPLETED,
                watchCount = 2,
            )
            movieProgressDao.insert(updatedProgress)

            val retrievedProgress = movieProgressDao.get(movieProgress.id)
            expectThat(retrievedProgress).isNotNull()
            expectThat(retrievedProgress!!.watchData.progress).isEqualTo(3600000L)
            expectThat(retrievedProgress.watchData.status).isEqualTo(WatchStatus.COMPLETED)
        }

    @Test
    fun shouldDeleteMovieProgress() =
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
            val movieProgress = MovieProgress(
                id = 1,
                filmId = "movie123",
                ownerId = 1,
                progress = 1800000L,
                status = WatchStatus.WATCHING,
                duration = 7200000L,
                watchedAt = Date(),
                watchCount = 1,
            )

            db.userDao().insert(user)
            db.filmsDao().insert(film)
            movieProgressDao.insert(movieProgress)

            movieProgressDao.delete(movieProgress.id)

            val retrievedProgress = movieProgressDao.get(movieProgress.id)
            expectThat(retrievedProgress).isNull()
        }

    @Test
    fun shouldHandleWatchCount() =
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
            val movieProgress = MovieProgress(
                id = 1,
                filmId = "movie123",
                ownerId = 1,
                progress = 7200000L,
                status = WatchStatus.COMPLETED,
                duration = 7200000L,
                watchedAt = Date(),
                watchCount = 3,
            )

            db.userDao().insert(user)
            db.filmsDao().insert(film)
            movieProgressDao.insert(movieProgress)

            val retrievedProgress = movieProgressDao.get(movieProgress.id)
            expectThat(retrievedProgress).isNotNull()
            expectThat(retrievedProgress!!.watchData.watchCount).isEqualTo(3)
            expectThat(retrievedProgress.watchData.status).isEqualTo(WatchStatus.COMPLETED)
        }
}
