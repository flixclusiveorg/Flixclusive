package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.EpisodeProgressDao
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.EpisodeProgress
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
class EpisodeProgressDaoTest {
    private lateinit var episodeProgressDao: EpisodeProgressDao
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).build()
        episodeProgressDao = db.episodeProgressDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun shouldInsertAndRetrieveEpisodeProgress() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val tvSeries = DBFilm(
                id = "series123",
                providerId = "provider1",
                title = "Test Series",
                adult = false,
                filmType = FilmType.TV_SHOW,
                rating = 9.0,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val episodeProgress = EpisodeProgress(
                id = 1,
                filmId = "series123",
                ownerId = 1,
                progress = 1800000L,
                duration = 3600000L,
                status = WatchStatus.WATCHING,
                watchedAt = Date(),
                seasonNumber = 1,
                episodeNumber = 1,
            )

            db.userDao().insert(user)
            db.filmsDao().insert(tvSeries)
            episodeProgressDao.insert(episodeProgress)

            val retrievedProgress = episodeProgressDao.get(episodeProgress.id)
            expectThat(retrievedProgress).isNotNull()
            expectThat(retrievedProgress!!.watchData.filmId).isEqualTo("series123")
            expectThat(retrievedProgress.watchData.seasonNumber).isEqualTo(1)
            expectThat(retrievedProgress.watchData.episodeNumber).isEqualTo(1)
            expectThat(retrievedProgress.watchData.progress).isEqualTo(1800000L)
        }

    @Test
    fun shouldReturnNullForNonexistentEpisodeProgress() =
        runTest {
            val result = episodeProgressDao.get(0)
            expectThat(result).isNull()
        }

    @Test
    fun shouldGetAllEpisodeProgressForUser() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val tvSeries = DBFilm(
                id = "series123",
                providerId = "provider1",
                title = "Test Series",
                adult = false,
                filmType = FilmType.TV_SHOW,
                rating = 9.0,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val episode1Progress = EpisodeProgress(
                id = 1,
                filmId = "series123",
                ownerId = 1,
                progress = 1800000L,
                duration = 3600000L,
                status = WatchStatus.COMPLETED,
                watchedAt = Date(System.currentTimeMillis() - 86400000),
                seasonNumber = 1,
                episodeNumber = 1,
            )
            val episode2Progress = EpisodeProgress(
                id = 2,
                filmId = "series123",
                ownerId = 1,
                progress = 900000L,
                duration = 3600000L,
                status = WatchStatus.WATCHING,
                watchedAt = Date(),
                seasonNumber = 1,
                episodeNumber = 2,
            )

            db.userDao().insert(user)
            db.filmsDao().insert(tvSeries)
            episodeProgressDao.insert(episode1Progress)
            episodeProgressDao.insert(episode2Progress)

            val allProgress = episodeProgressDao.getAllAsFlow(1).first()
            expectThat(allProgress).hasSize(1)
            expectThat(allProgress[0].watchData.episodeNumber).isEqualTo(2)
        }

    @Test
    fun shouldReturnEmptyListForUserWithNoEpisodeProgress() =
        runTest {
            val allProgress = episodeProgressDao.getAllAsFlow(999).first()
            expectThat(allProgress).isEmpty()
        }

    @Test
    fun shouldGetFurthestEpisodeProgressForSpecificSeries() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val series1 = DBFilm(
                id = "series1",
                providerId = "provider1",
                title = "Series 1",
                adult = false,
                filmType = FilmType.TV_SHOW,
                rating = 8.5,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val series2 = DBFilm(
                id = "series2",
                providerId = "provider1",
                title = "Series 2",
                adult = false,
                filmType = FilmType.TV_SHOW,
                rating = 9.0,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val series1Episode = EpisodeProgress(
                id = 1,
                filmId = "series1",
                ownerId = 1,
                progress = 1800000L,
                duration = 3600000L,
                status = WatchStatus.WATCHING,
                watchedAt = Date(),
                seasonNumber = 1,
                episodeNumber = 1,
            )
            val series2Episode = EpisodeProgress(
                id = 2,
                filmId = "series2",
                ownerId = 1,
                progress = 900000L,
                duration = 1800000L,
                status = WatchStatus.COMPLETED,
                watchedAt = Date(),
                seasonNumber = 1,
                episodeNumber = 1,
            )

            db.userDao().insert(user)
            db.filmsDao().insert(series1)
            db.filmsDao().insert(series2)
            episodeProgressDao.insert(series1Episode)
            episodeProgressDao.insert(series2Episode)

            val series1Progress = episodeProgressDao.getAsFlow("series1", 1).first()
            expectThat(series1Progress).isNotNull()
            expectThat(series1Progress!!.watchData.filmId).isEqualTo("series1")
        }

    @Test
    fun shouldUpdateEpisodeProgress() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val tvSeries = DBFilm(
                id = "series123",
                providerId = "provider1",
                title = "Test Series",
                adult = false,
                filmType = FilmType.TV_SHOW,
                rating = 9.0,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val episodeProgress = EpisodeProgress(
                id = 1,
                filmId = "series123",
                ownerId = 1,
                progress = 1800000L,
                duration = 3600000L,
                status = WatchStatus.WATCHING,
                watchedAt = Date(),
                seasonNumber = 1,
                episodeNumber = 1,
            )

            db.userDao().insert(user)
            db.filmsDao().insert(tvSeries)
            episodeProgressDao.insert(episodeProgress)

            val updatedProgress = episodeProgress.copy(
                progress = 3600000L,
                status = WatchStatus.COMPLETED,
            )
            episodeProgressDao.insert(updatedProgress)

            val retrievedProgress = episodeProgressDao.get(episodeProgress.id)
            expectThat(retrievedProgress).isNotNull()
            expectThat(retrievedProgress!!.watchData.progress).isEqualTo(3600000L)
            expectThat(retrievedProgress.watchData.status).isEqualTo(WatchStatus.COMPLETED)
        }

    @Test
    fun shouldDeleteEpisodeProgress() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val tvSeries = DBFilm(
                id = "series123",
                providerId = "provider1",
                title = "Test Series",
                adult = false,
                filmType = FilmType.TV_SHOW,
                rating = 9.0,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )
            val episodeProgress = EpisodeProgress(
                id = 1,
                filmId = "series123",
                ownerId = 1,
                progress = 1800000L,
                duration = 3600000L,
                status = WatchStatus.WATCHING,
                watchedAt = Date(),
                seasonNumber = 1,
                episodeNumber = 1,
            )

            db.userDao().insert(user)
            db.filmsDao().insert(tvSeries)
            episodeProgressDao.insert(episodeProgress)

            episodeProgressDao.delete(episodeProgress.id)

            val retrievedProgress = episodeProgressDao.get(episodeProgress.id)
            expectThat(retrievedProgress).isNull()
        }

    @Test
    fun shouldGetSeasonProgress() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val tvSeries = DBFilm(
                id = "series123",
                providerId = "provider1",
                title = "Test Series",
                adult = false,
                filmType = FilmType.TV_SHOW,
                rating = 9.0,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )

            db.userDao().insert(user)
            db.filmsDao().insert(tvSeries)

            val episodes = (1..5).map { episodeNum ->
                EpisodeProgress(
                    id = episodeNum.toLong(),
                    filmId = "series123",
                    ownerId = 1,
                    progress = 1800000L * episodeNum,
                    duration = 3600000L,
                    status = if (episodeNum <= 3) WatchStatus.COMPLETED else WatchStatus.WATCHING,
                    watchedAt = Date(System.currentTimeMillis() + episodeNum * 1000L),
                    seasonNumber = 1,
                    episodeNumber = episodeNum,
                )
            }

            episodes.forEach { episodeProgressDao.insert(it) }

            val seasonProgress = episodeProgressDao.getSeasonProgress("series123", 1, 1)
            expectThat(seasonProgress).hasSize(5)
            expectThat(seasonProgress.map { it.episodeNumber }).isEqualTo(listOf(1, 2, 3, 4, 5))
        }

    @Test
    fun shouldHandleMultipleSeasonsForSameSeries() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val tvSeries = DBFilm(
                id = "series123",
                providerId = "provider1",
                title = "Test Series",
                adult = false,
                filmType = FilmType.TV_SHOW,
                rating = 9.0,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )

            db.userDao().insert(user)
            db.filmsDao().insert(tvSeries)

            val season1Episode = EpisodeProgress(
                id = 1,
                filmId = "series123",
                ownerId = 1,
                progress = 3600000L,
                duration = 3600000L,
                status = WatchStatus.COMPLETED,
                watchedAt = Date(System.currentTimeMillis() - 86400000),
                seasonNumber = 1,
                episodeNumber = 1,
            )
            val season2Episode = EpisodeProgress(
                id = 2,
                filmId = "series123",
                ownerId = 1,
                progress = 1800000L,
                duration = 3600000L,
                status = WatchStatus.WATCHING,
                watchedAt = Date(),
                seasonNumber = 2,
                episodeNumber = 1,
            )

            episodeProgressDao.insert(season1Episode)
            episodeProgressDao.insert(season2Episode)

            val season1Progress = episodeProgressDao.getSeasonProgress("series123", 1, 1)
            val season2Progress = episodeProgressDao.getSeasonProgress("series123", 2, 1)

            expectThat(season1Progress).hasSize(1)
            expectThat(season1Progress[0].seasonNumber).isEqualTo(1)
            expectThat(season2Progress).hasSize(1)
            expectThat(season2Progress[0].seasonNumber).isEqualTo(2)
        }

    @Test
    fun shouldGetRandomEpisodeProgress() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            val tvSeries = DBFilm(
                id = "series123",
                providerId = "provider1",
                title = "Test Series",
                adult = false,
                filmType = FilmType.TV_SHOW,
                rating = 9.0,
                customProperties = emptyMap(),
                createdAt = Date(),
                updatedAt = Date(),
            )

            db.userDao().insert(user)
            db.filmsDao().insert(tvSeries)

            val episodes = (1..10).map { i ->
                EpisodeProgress(
                    id = i.toLong(),
                    filmId = "series123",
                    ownerId = 1,
                    progress = 1800000L,
                    duration = 3600000L,
                    status = WatchStatus.WATCHING,
                    watchedAt = Date(),
                    seasonNumber = 1,
                    episodeNumber = i,
                )
            }

            episodes.forEach { episodeProgressDao.insert(it) }

            val randomProgress = episodeProgressDao.getRandoms(1, 3).first()
            expectThat(randomProgress).hasSize(3)
        }
}
