package com.flixclusive.domain.provider.usecase.get.impl

import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.MediaLinkResourceState
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.provider.repository.CacheKey
import com.flixclusive.data.provider.repository.CachedLinks
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.tmdb.repository.TMDBWatchProvidersRepository
import com.flixclusive.domain.provider.util.extensions.getWatchId
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.Author
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Status
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.provider.ProviderApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class GetMediaLinksUseCaseImplTest {
    private lateinit var getMediaLinksUseCase: GetMediaLinksUseCaseImpl
    private lateinit var cachedLinksRepository: CachedLinksRepository
    private lateinit var tmdbWatchProvidersRepository: TMDBWatchProvidersRepository
    private lateinit var providerApiRepository: ProviderApiRepository
    private lateinit var providerRepository: ProviderRepository
    private lateinit var appDispatchers: AppDispatchers
    private val testDispatcher = StandardTestDispatcher()

    private val testMovie = Movie(
        id = "238",
        tmdbId = 238,
        imdbId = "tt0068646",
        title = "The Godfather",
        posterImage = "https://image.tmdb.org/t/p/w500/poster.jpg",
        backdropImage = "https://image.tmdb.org/t/p/w1280/backdrop.jpg",
        logoImage = null,
        year = 1972,
        genres = emptyList(),
        language = "en",
        rating = 8.7,
        overview = "The aging patriarch of an organized crime dynasty transfers control to his reluctant son.",
        recommendations = emptyList(),
        homePage = "https://www.thegodfather.com/",
        providerId = DEFAULT_FILM_SOURCE_NAME,
        runtime = 175,
        adult = false,
    )

    private val testTvShow = TvShow(
        id = "1399",
        tmdbId = 1399,
        imdbId = "tt0903747",
        title = "Game of Thrones",
        posterImage = "https://image.tmdb.org/t/p/w500/tv_poster.jpg",
        backdropImage = "https://image.tmdb.org/t/p/w1280/tv_backdrop.jpg",
        logoImage = null,
        year = 2011,
        genres = emptyList(),
        language = "en",
        rating = 9.0,
        overview = "Nine noble families fight for control over the lands of Westeros.",
        recommendations = emptyList(),
        homePage = "https://www.hbo.com/game-of-thrones",
        providerId = DEFAULT_FILM_SOURCE_NAME,
        adult = false,
        totalSeasons = 8,
        totalEpisodes = 73,
        seasons = emptyList(),
    )

    private val testEpisode = Episode(
        season = 1,
        number = 1,
        title = "Winter Is Coming",
        overview = "Eddard Stark is torn between his family and an old friend.",
        image = "https://image.tmdb.org/t/p/w500/episode.jpg",
        airDate = "2011-04-17",
        runtime = 62,
    )

    private val testProviderMetadata = ProviderMetadata(
        id = "test-provider",
        name = "Test Provider",
        authors = listOf(Author("Test Author")),
        repositoryUrl = "https://test.com",
        buildUrl = "https://test.com/build",
        changelog = "Test changelog",
        versionName = "1.0.0",
        versionCode = 1,
        description = "Test provider description",
        iconUrl = "https://test.com/icon.png",
        language = Language.Multiple,
        providerType = ProviderType.All,
        status = Status.Working,
    )

    private val testStream = Stream(
        url = "https://test.com/stream",
        name = "Test Stream",
    )

    private val testSubtitle = Subtitle(
        url = "https://test.com/subtitle",
        language = "en",
    )

    private val testCachedLinks = CachedLinks(
        watchId = "watch123",
        providerId = "test-provider",
        thumbnail = "thumbnail.jpg",
        streams = listOf(testStream),
        subtitles = listOf(testSubtitle),
    )

    @Before
    fun setup() {
        cachedLinksRepository = mockk(relaxed = true)
        tmdbWatchProvidersRepository = mockk()
        providerApiRepository = mockk()
        providerRepository = mockk()
        appDispatchers = createTestAppDispatchers(testDispatcher)

        // Mock the extension function
        mockkStatic("com.flixclusive.domain.provider.util.extensions.ProviderApiKt")

        getMediaLinksUseCase = GetMediaLinksUseCaseImpl(
            cachedLinksRepository = cachedLinksRepository,
            tmdbWatchProvidersRepository = tmdbWatchProvidersRepository,
            providerApiRepository = providerApiRepository,
            providerRepository = providerRepository,
            appDispatchers = appDispatchers,
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("com.flixclusive.domain.provider.util.extensions.ProviderApiKt")
    }

    private fun createTestAppDispatchers(testDispatcher: CoroutineDispatcher): AppDispatchers {
        return object : AppDispatchers {
            override val default: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
            override val unconfined: CoroutineDispatcher = testDispatcher

            override val ioScope: CoroutineScope get() = TestScope(testDispatcher)
            override val defaultScope: CoroutineScope get() = TestScope(testDispatcher)
            override val mainScope: CoroutineScope get() = TestScope(testDispatcher)
        }
    }

    @Test
    fun `invoke with movie should return success when cache exists with streamable links`() =
        runTest(testDispatcher) {
            val cacheKey = CacheKey.create(
                filmId = testMovie.identifier,
                providerId = "test-provider",
                episode = null,
            )
            val providerApi = mockk<ProviderApi>(relaxed = true)

            every { providerRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { providerApiRepository.getApi("test-provider") } returns providerApi
            every { cachedLinksRepository.getCache(cacheKey) } returns testCachedLinks
            every { cachedLinksRepository.currentCache } returns MutableStateFlow(testCachedLinks)

            getMediaLinksUseCase(
                movie = testMovie,
                providerId = "test-provider",
            ).test {
                // Cache hits should emit Success immediately without intermediate states
                val result = awaitItem()
                expectThat(result).isA<MediaLinkResourceState.Success>()
                awaitComplete()
            }
        }

    @Test
    fun `invoke with tv show and episode should return success when cache exists`() =
        runTest(testDispatcher) {
            val cacheKey = CacheKey.create(
                filmId = testTvShow.identifier,
                providerId = "test-provider",
                episode = testEpisode,
            )
            val providerApi = mockk<ProviderApi>(relaxed = true)

            every { providerRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { providerApiRepository.getApi("test-provider") } returns providerApi
            every { cachedLinksRepository.getCache(cacheKey) } returns testCachedLinks
            every { cachedLinksRepository.currentCache } returns MutableStateFlow(testCachedLinks)

            getMediaLinksUseCase(testTvShow, testEpisode, "test-provider").test {
                // Cache hits should emit Success immediately without intermediate states
                val result = awaitItem()
                expectThat(result).isA<MediaLinkResourceState.Success>()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `invoke should return success with trusted providers when no providers available and film is from TMDB`() =
        runTest(testDispatcher) {
            val tmdbStreams = listOf(testStream)
            val cacheKey = CacheKey.create(
                filmId = testMovie.identifier,
                providerId = DEFAULT_FILM_SOURCE_NAME,
                episode = null,
            )

            every { providerRepository.getOrderedProviders() } returns emptyList()
            every { cachedLinksRepository.getCache(cacheKey) } returns null
            coEvery {
                tmdbWatchProvidersRepository.getWatchProviders(
                    mediaType = "movie",
                    id = testMovie.tmdbId!!,
                )
            } returns Resource.Success(tmdbStreams)

            getMediaLinksUseCase(testMovie).test {
                // TMDB flow should emit SuccessWithTrustedProviders immediately
                val result = awaitItem()
                expectThat(result).isA<MediaLinkResourceState.SuccessWithTrustedProviders>()
                awaitComplete()
            }

            verify {
                cachedLinksRepository.storeCache(
                    key = cacheKey,
                    cachedLinks = CachedLinks(
                        watchId = testMovie.identifier,
                        providerId = DEFAULT_FILM_SOURCE_NAME,
                        thumbnail = testMovie.backdropImage,
                        streams = tmdbStreams,
                    ),
                )
            }
        }

    @Test
    fun `invoke should return error when TMDB watch providers fail`() =
        runTest(testDispatcher) {
            val errorMessage = UiText.StringValue("TMDB error")
            val cacheKey = CacheKey.create(
                filmId = testMovie.identifier,
                providerId = DEFAULT_FILM_SOURCE_NAME,
                episode = null,
            )

            every { providerRepository.getOrderedProviders() } returns emptyList()
            every { cachedLinksRepository.getCache(cacheKey) } returns null
            coEvery {
                tmdbWatchProvidersRepository.getWatchProviders(
                    mediaType = "movie",
                    id = testMovie.tmdbId!!,
                )
            } returns Resource.Failure(errorMessage)

            getMediaLinksUseCase(testMovie).test {
                // TMDB failure should emit Error immediately
                val result = awaitItem()
                expectThat(result).isA<MediaLinkResourceState.Error>()
                expectThat((result as MediaLinkResourceState.Error).message).isEqualTo(errorMessage)
                awaitComplete()
            }
        }

    @Test
    fun `invoke should return unavailable when provider API not found`() =
        runTest(testDispatcher) {
            val nonTmdbMovie = testMovie.copy(tmdbId = null, providerId = "missing-provider")

            every { providerRepository.getOrderedProviders() } returns emptyList()
            every { providerApiRepository.getApi("missing-provider") } returns null

            getMediaLinksUseCase(
                movie = nonTmdbMovie,
                providerId = "missing-provider",
            ).test {
                // Provider not found should emit Unavailable immediately
                val result = awaitItem()
                expectThat(result).isA<MediaLinkResourceState.Unavailable>()
                awaitComplete()
            }
        }

    @Test
    fun `invoke should fetch watch id and extract links when cache not available`() =
        runTest(testDispatcher) {
            val watchId = "fetched-watch-id"
            val providerApi = mockk<ProviderApi>(relaxed = true)
            val cacheKey = CacheKey.create(
                filmId = testMovie.identifier,
                providerId = "test-provider",
                episode = null,
            )

            every { providerRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { providerRepository.getProviderMetadata("test-provider") } returns testProviderMetadata
            every { providerApiRepository.getApi("test-provider") } returns providerApi
            every { cachedLinksRepository.getCache(cacheKey) } returns null
            every { cachedLinksRepository.currentCache } returns MutableStateFlow(testCachedLinks)
            coEvery { providerApi.getWatchId(testMovie) } returns Resource.Success(watchId)
            coEvery {
                providerApi.getLinks(
                    watchId = watchId,
                    film = testMovie,
                    episode = null,
                    onLinkFound = any(),
                )
            } returns Unit

            advanceUntilIdle()

            getMediaLinksUseCase(testMovie).test {
                // This flow will emit: Fetching -> Extracting -> Success
                // Skip intermediate states and verify final state
                var currentState: MediaLinkResourceState
                do {
                    currentState = awaitItem()
                } while (!currentState.isSuccess)

                expectThat(currentState).isA<MediaLinkResourceState.Success>()
                awaitComplete()
            }

            verify {
                cachedLinksRepository.storeCache(
                    key = cacheKey,
                    cachedLinks = CachedLinks(
                        watchId = watchId,
                        providerId = "test-provider",
                        thumbnail = testMovie.backdropImage,
                        episode = null,
                    ),
                )
            }
            coVerify { providerApi.getWatchId(testMovie) }
            coVerify {
                providerApi.getLinks(
                    watchId = watchId,
                    film = testMovie,
                    episode = null,
                    onLinkFound = any(),
                )
            }
        }

    @Test
    fun `invoke should return error when watch id fetch fails`() =
        runTest(testDispatcher) {
            val errorMessage = UiText.StringValue("Watch ID fetch failed")
            val providerApi = mockk<ProviderApi>(relaxed = true)
            val cacheKey = CacheKey.create(
                filmId = testMovie.identifier,
                providerId = "test-provider",
                episode = null,
            )

            every { providerRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { providerRepository.getProviderMetadata("test-provider") } returns testProviderMetadata
            every { providerApiRepository.getApi("test-provider") } returns providerApi
            every { cachedLinksRepository.getCache(cacheKey) } returns null
            coEvery { providerApi.getWatchId(testMovie) } returns Resource.Failure(errorMessage)

            getMediaLinksUseCase(testMovie).test {
                // This flow will emit: Fetching -> Error
                // Skip intermediate states and verify final state
                var currentState: MediaLinkResourceState
                do {
                    currentState = awaitItem()
                } while (currentState.isLoading)

                expectThat(currentState).isA<MediaLinkResourceState.Error>()
                expectThat((currentState as MediaLinkResourceState.Error).message).isEqualTo(errorMessage)
                awaitComplete()
            }
        }

    @Test
    fun `invoke should return error for non-TMDB film without watch id`() =
        runTest(testDispatcher) {
            val nonTmdbMovie = testMovie.copy(tmdbId = null, providerId = "test-provider")
            val providerApi = mockk<ProviderApi>(relaxed = true)
            val cacheKey = CacheKey.create(
                filmId = nonTmdbMovie.identifier,
                providerId = "test-provider",
                episode = null,
            )

            every { providerRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { providerRepository.getProviderMetadata("test-provider") } returns testProviderMetadata
            every { providerApiRepository.getApi("test-provider") } returns providerApi
            every { cachedLinksRepository.getCache(cacheKey) } returns null

            getMediaLinksUseCase(nonTmdbMovie).test {
                // This flow will emit: Fetching -> Extracting -> Success
                // Skip intermediate states and verify final state
                var currentState: MediaLinkResourceState
                do {
                    currentState = awaitItem()
                } while (!currentState.isError)

                expectThat(currentState).isA<MediaLinkResourceState.Error>()
                awaitComplete()
            }
        }

    @Test
    fun `invoke should use provided watch id for extraction`() =
        runTest(testDispatcher) {
            val providedWatchId = "provided-watch-id"
            val providerApi = mockk<ProviderApi>(relaxed = true)
            val cacheKey = CacheKey.create(
                filmId = testMovie.identifier,
                providerId = "test-provider",
                episode = null,
            )

            every { providerRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { providerRepository.getProviderMetadata("test-provider") } returns testProviderMetadata
            every { providerApiRepository.getApi("test-provider") } returns providerApi
            every { cachedLinksRepository.getCache(cacheKey) } returns null
            every { cachedLinksRepository.currentCache } returns MutableStateFlow(testCachedLinks)
            coEvery {
                providerApi.getLinks(
                    watchId = providedWatchId,
                    film = testMovie,
                    episode = null,
                    onLinkFound = any(),
                )
            } returns Unit

            getMediaLinksUseCase(testMovie, providedWatchId, null, "test-provider").test {
                // This flow will emit: Extracting -> Success (skips Fetching since watchId is provided)
                // Skip intermediate states and verify final state
                var currentState: MediaLinkResourceState
                do {
                    currentState = awaitItem()
                } while (!currentState.isSuccess)

                expectThat(currentState).isA<MediaLinkResourceState.Success>()
                awaitComplete()
            }

            coVerify {
                providerApi.getLinks(
                    watchId = providedWatchId,
                    film = testMovie,
                    episode = null,
                    onLinkFound = any(),
                )
            }
            coVerify(exactly = 0) { providerApi.getWatchId(any()) }
        }

    @Test
    fun `invoke should return error when no stream links are found`() =
        runTest(testDispatcher) {
            val watchId = "watch-id"
            val providerApi = mockk<ProviderApi>(relaxed = true)
            val cacheKey = CacheKey.create(
                filmId = testMovie.identifier,
                providerId = "test-provider",
                episode = null,
            )
            val emptyCache = CachedLinks(
                watchId = watchId,
                providerId = "test-provider",
                streams = emptyList(),
            )

            every { providerRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { providerRepository.getProviderMetadata("test-provider") } returns testProviderMetadata
            every { providerApiRepository.getApi("test-provider") } returns providerApi
            every { cachedLinksRepository.getCache(cacheKey) } returns null
            every { cachedLinksRepository.currentCache } returns MutableStateFlow(emptyCache)
            coEvery { providerApi.getWatchId(testMovie) } returns Resource.Success(watchId)
            coEvery {
                providerApi.getLinks(
                    watchId = watchId,
                    film = testMovie,
                    episode = null,
                    onLinkFound = any(),
                )
            } returns Unit

            getMediaLinksUseCase(testMovie).test {
                // This flow will emit: Fetching -> Extracting -> Error
                // Skip intermediate states and verify final state
                var currentState: MediaLinkResourceState
                do {
                    currentState = awaitItem()
                } while (currentState.isLoading)

                expectThat(currentState).isA<MediaLinkResourceState.Error>()
                awaitComplete()
            }
        }
}
