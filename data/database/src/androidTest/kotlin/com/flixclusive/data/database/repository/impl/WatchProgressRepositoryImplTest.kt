package com.flixclusive.data.database.repository.impl

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.model.film.util.FilmType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

@RunWith(AndroidJUnit4::class)
class WatchProgressRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: WatchProgressRepositoryImpl
    private lateinit var appDispatchers: AppDispatchers

    private val testDispatcher = StandardTestDispatcher()
    private val testDBFilm = DatabaseTestDefaults.getDBFilm()
    private val testUser = DatabaseTestDefaults.getUser()
    private val testMovieProgress = DatabaseTestDefaults.getMovieProgress(
        filmId = testDBFilm.id,
        ownerId = testUser.id,
    )
    private val testEpisodeProgress = DatabaseTestDefaults.getEpisodeProgress(
        filmId = testDBFilm.id,
        ownerId = testUser.id,
    )

    @Before
    fun setUp() {
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
        database = DatabaseTestDefaults.createDatabase(
            context = ApplicationProvider.getApplicationContext(),
        )
        repository = WatchProgressRepositoryImpl(
            movieProgressDao = database.movieProgressDao(),
            episodeProgressDao = database.episodeProgressDao(),
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
    fun shouldInsertWatchHistoryItem() =
        runTest(testDispatcher) {
            val itemId = repository.insert(testMovieProgress, testDBFilm)

            repository.getAllAsFlow(testMovieProgress.ownerId).test {
                val result = awaitItem()
                expectThat(result).hasSize(1).and {
                    get { first() }.isA<MovieProgressWithMetadata>().and {
                        get { id }.isEqualTo(itemId)
                        get { film.title }.isEqualTo(testDBFilm.title)
                        get { watchData.ownerId }.isEqualTo(testMovieProgress.ownerId)
                    }
                }
            }
        }

    @Test
    fun shouldRetrieveWatchHistoryItemById() =
        runTest(testDispatcher) {
            val itemId = repository.insert(testEpisodeProgress, testDBFilm)
            val retrievedItem = repository.get(
                id = itemId,
                type = FilmType.TV_SHOW,
            )

            expectThat(retrievedItem).isNotNull().and {
                get { id }.isEqualTo(itemId)
                get { film.title }.isEqualTo(testDBFilm.title)
                get { watchData.watchedAt }.isEqualTo(testMovieProgress.watchedAt)
            }
        }

    @Test
    fun shouldObserveWatchHistoryItemById() =
        runTest(testDispatcher) {
            val itemId = repository.insert(testEpisodeProgress, testDBFilm)

            repository
                .getAsFlow(
                    id = itemId,
                    type = FilmType.TV_SHOW,
                ).test {
                    expectThat(awaitItem()).isNotNull().and {
                        get { id }.isEqualTo(itemId)
                        get { film.title }.isEqualTo(testDBFilm.title)
                    }
                }
        }

    @Test
    fun shouldObserveWatchHistoryItemByFilmId() =
        runTest(testDispatcher) {
            val itemId = repository.insert(testEpisodeProgress, testDBFilm)

            repository
                .getAsFlow(
                    id = testDBFilm.id,
                    ownerId = testEpisodeProgress.ownerId,
                    type = FilmType.TV_SHOW,
                ).test {
                    expectThat(awaitItem()).isNotNull().and {
                        get { id }.isEqualTo(itemId)
                        get { film.title }.isEqualTo(testDBFilm.title)
                    }
                }
        }

    @Test
    fun shouldRetrieveAllItemsByOwnerId() =
        runTest(testDispatcher) {
            database.userDao().insert(testUser.copy(id = 2))
            val item1 = testMovieProgress.copy(id = 1)
            val item2 = testMovieProgress.copy(filmId = "item2")
            val item3 = testMovieProgress.copy(filmId = "item3", ownerId = 2)

            repository.insert(item1, testDBFilm)
            repository.insert(item2, testDBFilm.copy(id = "item2"))
            repository.insert(item3, testDBFilm.copy(id = "item3"))

            repository.getAllAsFlow(testMovieProgress.ownerId).test {
                val result = awaitItem()
                expectThat(result).hasSize(2)
            }
        }

    @Test
    fun shouldGetRandomWatchHistoryItems() =
        runTest(testDispatcher) {
            val item1 = testMovieProgress.copy(id = 1)
            val item2 = testMovieProgress.copy(filmId = "item2")
            val item3 = testMovieProgress.copy(filmId = "item3")

            repository.insert(item1, testDBFilm)
            repository.insert(item2, testDBFilm.copy(id = "item2"))
            repository.insert(item3, testDBFilm.copy(id = "item3"))

            repository.getRandoms(testMovieProgress.ownerId, 2).test {
                expectThat(awaitItem()).hasSize(2)
            }
        }

    @Test
    fun shouldDeleteWatchHistoryItemById() =
        runTest(testDispatcher) {
            val itemId = repository.insert(testMovieProgress, testDBFilm)

            repository.delete(itemId, testDBFilm.filmType)

            val retrievedItem = repository.get(
                itemId,
                testDBFilm.filmType,
            )
            expectThat(retrievedItem).isNull()
        }

    @Test
    fun shouldRemoveAllWatchHistoryForOwner() =
        runTest(testDispatcher) {
            database.userDao().insert(testUser.copy(id = 2))
            val item1 = testMovieProgress.copy(id = 1)
            val item2 = testMovieProgress.copy(id = 2)
            val item3 = testMovieProgress.copy(id = 3, ownerId = 2)

            repository.insert(item1, testDBFilm)
            repository.insert(item2, testDBFilm)
            repository.insert(item3, testDBFilm)

            repository.removeAll(testMovieProgress.ownerId)

            turbineScope {
                val firstOwner = repository.getAllAsFlow(testMovieProgress.ownerId).testIn(this)
                val secondOwner = repository.getAllAsFlow(2).testIn(this)

                expectThat(firstOwner.awaitItem()).isEmpty()
                expectThat(secondOwner.awaitItem()).hasSize(1)

                firstOwner.cancelAndIgnoreRemainingEvents()
                secondOwner.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun shouldReturnNullForNonExistentItem() =
        runTest(testDispatcher) {
            val retrievedItem = repository.get(0, FilmType.TV_SHOW)
            expectThat(retrievedItem).isNull()
        }
}
