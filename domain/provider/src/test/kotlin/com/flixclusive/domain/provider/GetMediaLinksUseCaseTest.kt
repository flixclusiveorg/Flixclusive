package com.flixclusive.domain.provider

import app.cash.turbine.test
import com.flixclusive.core.database.entity.EpisodeWatched
import com.flixclusive.core.database.entity.WatchHistory
import com.flixclusive.core.database.entity.toDBFilm
import com.flixclusive.core.strings.UiText
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.domain.provider.fake.FakeCachedLinksRepository
import com.flixclusive.domain.provider.fake.FakeProviderApiRepository
import com.flixclusive.domain.provider.fake.FakeProviderRepository
import com.flixclusive.domain.provider.fake.FakeTMDBRepository
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.Stream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

const val DEFAULT_FILM_ID = "default-film-id"

class GetMediaLinksUseCaseTest {
    @get:Rule
    val logRule: LogRule = LogRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var useCase: GetMediaLinksUseCase

    private lateinit var fakeCachedLinksRepository: FakeCachedLinksRepository
    private lateinit var fakeTmdbRepository: FakeTMDBRepository
    private lateinit var fakeProviderApiRepository: FakeProviderApiRepository
    private lateinit var fakeProviderRepository: FakeProviderRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        fakeCachedLinksRepository = FakeCachedLinksRepository()
        fakeTmdbRepository = FakeTMDBRepository()
        fakeProviderApiRepository = FakeProviderApiRepository()
        fakeProviderRepository = FakeProviderRepository()

        useCase = GetMediaLinksUseCase(
            cachedLinksRepository = fakeCachedLinksRepository,
            tmdbRepository = fakeTmdbRepository,
            providerApiRepository = fakeProviderApiRepository,
            providerRepository = fakeProviderRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `returns success immediately when cached links exist for movie`() =
        runTest(testDispatcher) {
            setupMockProviders()

            val movie = createMockMovie()
            val mockStreams = listOf(createMockStream())
            fakeCachedLinksRepository.addMockCache(
                filmId = movie.id!!,
                watchId = movie.id!!,
                providerId = "provider-1",
                streams = mockStreams,
            )

            var onSuccessCalled = false
            var episodeFromCallback: Episode? = Episode(id = "invalid")

            useCase(
                film = movie,
                watchHistoryItem = null,
                onSuccess = { episode ->
                    onSuccessCalled = true
                    episodeFromCallback = episode
                },
            ).test {
                expectThat(awaitItem()).isA<MediaLinkResourceState.Fetching>()
                expectThat(awaitItem()).isA<MediaLinkResourceState.Success>()
                expectThat(onSuccessCalled).isTrue()
                expectThat(episodeFromCallback).isNull()
                awaitComplete()
            }
        }

    @Test
    fun `returns success immediately when cached links exist for tv show episode`() =
        runTest(testDispatcher) {
            val tvShow = createMockTvShow()
            val episode = createMockEpisode()
            val mockStreams = listOf(createMockStream())
            setupMockProviders()

            fakeCachedLinksRepository.addMockCache(
                filmId = tvShow.id!!,
                providerId = "test-provider",
                episode = episode,
                streams = mockStreams,
            )

            var episodeFromCallback: Episode? = null

            useCase(
                film = tvShow,
                watchHistoryItem = null,
                episode = episode,
                onSuccess = { callbackEpisode ->
                    episodeFromCallback = callbackEpisode
                },
            ).test {
                expectThat(awaitItem()).isA<MediaLinkResourceState.Fetching>()
                expectThat(awaitItem()).isA<MediaLinkResourceState.Extracting>()
                expectThat(awaitItem()).isA<MediaLinkResourceState.Success>()
                expectThat(episodeFromCallback).isNotNull().get { id }.isEqualTo(episode.id)
                awaitComplete()
            }
        }

    @Test
    fun `fetches episode when tv show provided without episode`() =
        runTest(testDispatcher) {
            val tvShow = createMockTvShow()
            val watchHistory = createMockWatchHistory(tvShow)
            val expectedEpisode = createMockEpisode(season = 1, number = 2)

            fakeTmdbRepository.addMockSeason(
                1,
                listOf(
                    createMockEpisode(season = 1, number = 1),
                    expectedEpisode,
                ),
            )

            var onSuccessCalled = false
            var onErrorCalled = false

            useCase(
                film = tvShow,
                watchHistoryItem = watchHistory,
                onSuccess = { onSuccessCalled = true },
                onError = { onErrorCalled = true },
            ).test {
                val fetchingState = awaitItem()
                expectThat(fetchingState).isA<MediaLinkResourceState.Fetching>()

                advanceUntilIdle()

                val finalState = awaitItem()
                expectThat(finalState).isA<MediaLinkResourceState.Error>()
                expectThat(onSuccessCalled).isFalse()
                expectThat(onErrorCalled).isTrue()
                awaitComplete()
            }
        }

    @Test
    fun `returns error when episode fetching fails for tv show`() =
        runTest(testDispatcher) {
            val tvShow = createMockTvShow()
            fakeTmdbRepository.setShouldReturnError(true, "Season not found")

            var errorCalled = false
            var errorMessage: UiText? = null

            useCase(
                film = tvShow,
                watchHistoryItem = null,
                onSuccess = { },
                onError = { error ->
                    errorCalled = true
                    errorMessage = error
                },
            ).test {
                val fetchingState = awaitItem()
                expectThat(fetchingState).isA<MediaLinkResourceState.Fetching>()

                advanceUntilIdle()

                val errorState = awaitItem()
                expectThat(errorState).isA<MediaLinkResourceState.Error>()
                expectThat(errorCalled).isTrue()
                expectThat(errorMessage).isNotNull()
                awaitComplete()
            }
        }

    @Test
    fun `extracts from tmdb when no providers available and film is from tmdb`() =
        runTest(testDispatcher) {
            val movie = createMockMovie(isFromTmdb = true)

            useCase(
                film = movie,
                watchHistoryItem = null,
                onSuccess = { /*Do nothing*/ },
            ).test {
                expectThat(awaitItem()).isA<MediaLinkResourceState.SuccessWithTrustedProviders>()
                awaitComplete()
            }
        }

    @Test
    fun `returns unavailable when no providers and film not from tmdb`() =
        runTest(testDispatcher) {
            val movie = createMockMovie(isFromTmdb = false)

            var errorCalled = false
            var errorMessage: UiText? = null

            useCase(
                film = movie,
                watchHistoryItem = null,
                onSuccess = { },
                onError = { error ->
                    errorCalled = true
                    errorMessage = error
                },
            ).test {
                // When no providers and film not from TMDB,
                // the use case immediately returns Unavailable
                val result = awaitItem()
                expectThat(result).isA<MediaLinkResourceState.Unavailable>()
                expectThat(errorCalled).isTrue()
                expectThat(errorMessage).isNotNull()
                awaitComplete()
            }
        }

    @Test
    fun `prioritizes preferred provider when specified`() =
        runTest(testDispatcher) {
            val movie = createMockMovie(isFromTmdb = true)
            val preferredProviderId = "provider-2"
            setupMockProviders()

            useCase(
                film = movie,
                watchHistoryItem = null,
                preferredProvider = preferredProviderId,
                onSuccess = { /*Do nothing*/ },
            ).test {
                expectThat(awaitItem()).isA<MediaLinkResourceState.Fetching>()
                expectThat(awaitItem()).isA<MediaLinkResourceState.Extracting>()
                expectThat(awaitItem()).isA<MediaLinkResourceState.Success>()
                awaitComplete()
            }
        }

    @Test
    fun `skips providers that do not match film provider when film not from tmdb`() =
        runTest(testDispatcher) {
            val movie = createMockMovie(isFromTmdb = false, providerId = "specific-provider")
            setupMockProviders()

            var onSuccessCalled = false
            var onErrorCalled = false

            useCase(
                film = movie,
                watchHistoryItem = null,
                onSuccess = { onSuccessCalled = true },
                onError = { onErrorCalled = true },
            ).test {
                // When film has specific provider ID that doesn't match any available providers,
                // all providers are skipped and it returns Unavailable
                val result = awaitItem()
                expectThat(result).isA<MediaLinkResourceState.Unavailable>()
                expectThat(onSuccessCalled).isFalse()
                expectThat(onErrorCalled).isFalse()
                awaitComplete()
            }
        }

    @Test
    fun `returns error when all providers fail to extract links`() =
        runTest(testDispatcher) {
            val movie = createMockMovie(isFromTmdb = true).copy(
                id = "test-movie-id",
            )
            setupMockProviders()

            var onSuccessCalled = false

            useCase(
                film = movie,
                watchHistoryItem = null,
                onSuccess = { onSuccessCalled = true },
            ).test {
                skipItems(2)
                expectThat(awaitItem()).isA<MediaLinkResourceState.Error>()
                expectThat(onSuccessCalled).isFalse()
                awaitComplete()
            }
        }

    @Test
    fun `passes watch id parameter correctly`() =
        runTest(testDispatcher) {
            val movie = createMockMovie(isFromTmdb = true)
            setupMockProviders()

            useCase(
                film = movie,
                watchHistoryItem = null,
                watchId = DEFAULT_FILM_ID,
                onSuccess = { /*Do nothing*/ },
            ).test {
                expectThat(awaitItem()).isA<MediaLinkResourceState.Fetching>()
                expectThat(awaitItem()).isA<MediaLinkResourceState.Extracting>()
                expectThat(awaitItem()).isA<MediaLinkResourceState.Success>()
                awaitComplete()
            }
        }

    @Test
    fun `returns success when cached links exist for specific provider`() =
        runTest(testDispatcher) {
            setupMockProviders()

            val movie = createMockMovie(isFromTmdb = false)
            val mockStreams = listOf(createMockStream())
            fakeCachedLinksRepository.addMockCache(
                filmId = movie.id!!,
                providerId = "specific-provider",
                watchId = movie.id!!,
                streams = mockStreams,
            )

            var onSuccessCalled = false

            useCase(
                film = movie,
                watchHistoryItem = null,
                preferredProvider = "specific-provider",
                onSuccess = { onSuccessCalled = true },
            ).test {
                val result = awaitItem()
                expectThat(result).isA<MediaLinkResourceState.Success>()
                expectThat(onSuccessCalled).isTrue()
                awaitComplete()
            }
        }

    private fun createMockMovie(
        isFromTmdb: Boolean = true,
        providerId: String = if (isFromTmdb) DEFAULT_FILM_SOURCE_NAME else "specific-provider",
    ): Movie {
        return Movie(
            id = DEFAULT_FILM_ID,
            tmdbId = if (isFromTmdb) 123 else null,
            title = "Mock Movie",
            releaseDate = "2023-01-01",
            overview = "Mock movie overview",
            runtime = 120,
            rating = 8.0,
            genres = emptyList(),
            language = "en",
            posterImage = null,
            homePage = null,
            providerId = providerId,
        )
    }

    private fun createMockTvShow(isFromTmdb: Boolean = true): TvShow {
        return TvShow(
            id = DEFAULT_FILM_ID,
            tmdbId = if (isFromTmdb) 456 else null,
            title = "Mock TV Show",
            releaseDate = "2023-01-01",
            overview = "Mock TV show overview",
            runtime = 45,
            rating = 8.5,
            genres = emptyList(),
            language = "en",
            totalSeasons = 3,
            totalEpisodes = 30,
            posterImage = null,
            homePage = null,
            providerId = if (isFromTmdb) DEFAULT_FILM_SOURCE_NAME else "specific-provider",
        )
    }

    private fun createMockEpisode(
        season: Int = 1,
        number: Int = 1,
    ): Episode {
        return Episode(
            id = "$season-$number",
            number = number,
            season = season,
            title = "Episode $number",
            image = null,
            rating = 8.0,
            runtime = 45,
        )
    }

    private fun createMockWatchHistory(film: Film): WatchHistory {
        val filmId = film.id ?: throw IllegalArgumentException("Film ID cannot be null")

        return WatchHistory(
            id = "$filmId-watch-history",
            ownerId = 1,
            episodesWatched = listOf(
                EpisodeWatched(
                    episodeId = "$filmId-1-1",
                    seasonNumber = 1,
                    episodeNumber = 1,
                    watchTime = System.currentTimeMillis() - 10000,
                ),
            ),
            seasons = 1,
            episodes = mapOf(1 to 1),
            film = film.toDBFilm(),
        )
    }

    companion object {
        fun createMockStream(): Stream {
            return Stream(
                url = "https://example.com/stream.m3u8",
                name = "Test Stream",
                description = "Test stream description",
            )
        }
    }

    private suspend fun setupMockProviders() {
        fakeProviderRepository.addMockProvider("provider-1", "Provider 1", true)
        fakeProviderRepository.addMockProvider("provider-2", "Provider 2", true)
        fakeProviderRepository.addMockProvider("provider-3", "Provider 3", false)

        fakeProviderApiRepository.addMockApi("provider-1")
        fakeProviderApiRepository.addMockApi("provider-2")
    }
}
