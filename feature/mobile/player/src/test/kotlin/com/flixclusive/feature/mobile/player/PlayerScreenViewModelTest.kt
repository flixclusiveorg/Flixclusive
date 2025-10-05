package com.flixclusive.feature.mobile.player

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.model.track.MediaSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.CacheKey
import com.flixclusive.data.provider.repository.CachedLinks
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.database.usecase.SetWatchProgressUseCase
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.domain.provider.usecase.get.GetEpisodeUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import strikt.assertions.isNotNull
import strikt.assertions.isNull

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerScreenViewModelTest {
    private lateinit var viewModel: PlayerScreenViewModel
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var cachedLinksRepository: CachedLinksRepository
    private lateinit var getEpisode: GetEpisodeUseCase
    private lateinit var getMediaLinks: GetMediaLinksUseCase
    private lateinit var getSeasonWithWatchProgress: GetSeasonWithWatchProgressUseCase
    private lateinit var providerRepository: ProviderRepository
    private lateinit var setWatchProgress: SetWatchProgressUseCase
    private lateinit var userSessionManager: UserSessionManager
    private lateinit var watchProgressRepository: WatchProgressRepository
    private lateinit var player: AppPlayer
    private lateinit var savedStateHandle: SavedStateHandle

    @get:Rule
    val logRule = LogRule()

    private val testDispatcher = StandardTestDispatcher()

    private val testUser = User(
        id = 1,
        name = "Test User",
        image = 0,
        pin = null,
        pinHint = null,
    )

    private val testMovie = FilmTestDefaults.getMovie(
        id = "movie123",
        title = "Test Movie",
    )

    private val testEpisode = FilmTestDefaults.getEpisode(
        id = "episode1",
        number = 1,
        season = 1,
        title = "Pilot",
    )

    private val testTvShow = FilmTestDefaults.getTvShow(
        id = "tvshow123",
        title = "Test TV Show",
    )

    private val testProviderId = ProviderTestDefaults.getProviderMetadata().id

    private val testStream = Stream(
        url = "https://example.com/stream.m3u8",
        name = "720p",
    )

    private val testSubtitle = Subtitle(
        url = "https://example.com/subtitle.srt",
        language = "en",
    )

    private val testCachedLinks = CachedLinks(
        watchId = "watch123",
        providerId = testProviderId,
        streams = listOf(testStream),
        subtitles = listOf(testSubtitle),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(::infoLog)
        every { infoLog(any()) } answers {
            println(args[0])
            1
        }

        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
        cachedLinksRepository = mockk(relaxed = true)
        getEpisode = mockk()
        getMediaLinks = mockk()
        getSeasonWithWatchProgress = mockk()
        providerRepository = mockk()
        setWatchProgress = mockk()
        userSessionManager = mockk()
        watchProgressRepository = mockk()
        player = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()

        setupDefaultBehavior()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(::infoLog)
    }

    private fun setupDefaultBehavior() {
        every { userSessionManager.currentUser } returns MutableStateFlow(testUser)
        every { cachedLinksRepository.currentCache } returns MutableStateFlow(testCachedLinks)
        every { providerRepository.getEnabledProviders() } returns listOf(
            ProviderTestDefaults.getProviderMetadata(id = testProviderId),
        )
        every { player.release() } just Runs
        every { player.currentPosition } returns 0L
        every { player.duration } returns 100000L
        every { player.addSubtitle(any()) } just Runs
        every { player.switchMediaSource(any()) } returns false
        every { player.prepare(any(), any(), any(), any(), any()) } just Runs

        coEvery { setWatchProgress(any()) } just Runs
    }

    private fun createViewModelForMovie() {
        savedStateHandle["film"] = testMovie
        savedStateHandle["providerId"] = testProviderId
        savedStateHandle["episode"] = null

        val movieProgress = MovieProgress(
            filmId = testMovie.identifier,
            ownerId = testUser.id,
            progress = 0L,
            status = WatchStatus.WATCHING,
        )

        coEvery {
            watchProgressRepository.get(
                id = testMovie.identifier,
                type = testMovie.filmType,
                ownerId = testUser.id,
            )
        } returns MovieProgressWithMetadata(
            watchData = movieProgress,
            film = testMovie.toDBFilm(),
        )

        every {
            watchProgressRepository.getAsFlow(
                id = testMovie.identifier,
                type = testMovie.filmType,
                ownerId = testUser.id,
            )
        } returns flowOf(
            MovieProgressWithMetadata(
                watchData = movieProgress,
                film = testMovie.toDBFilm(),
            ),
        )

        viewModel = PlayerScreenViewModel(
            appDispatchers = appDispatchers,
            cachedLinksRepository = cachedLinksRepository,
            getEpisode = getEpisode,
            getMediaLinks = getMediaLinks,
            getSeasonWithWatchProgress = getSeasonWithWatchProgress,
            providerRepository = providerRepository,
            setWatchProgress = setWatchProgress,
            userSessionManager = userSessionManager,
            watchProgressRepository = watchProgressRepository,
            player = player,
            savedStateHandle = savedStateHandle,
        )
    }

    private fun createViewModelForTvShow(episode: Episode = testEpisode) {
        savedStateHandle["film"] = testTvShow
        savedStateHandle["providerId"] = testProviderId
        savedStateHandle["episode"] = episode

        val episodeProgress = EpisodeProgress(
            filmId = testTvShow.identifier,
            ownerId = testUser.id,
            progress = 0L,
            status = WatchStatus.WATCHING,
            seasonNumber = episode.season,
            episodeNumber = episode.number,
        )

        coEvery {
            watchProgressRepository.get(
                id = testTvShow.identifier,
                type = testTvShow.filmType,
                ownerId = testUser.id,
            )
        } returns EpisodeProgressWithMetadata(
            watchData = episodeProgress,
            film = testTvShow.toDBFilm(),
        )

        every {
            watchProgressRepository.getAsFlow(
                id = testTvShow.identifier,
                type = testTvShow.filmType,
                ownerId = testUser.id,
            )
        } returns flowOf(
            EpisodeProgressWithMetadata(
                watchData = episodeProgress,
                film = testTvShow.toDBFilm(),
            ),
        )

        coEvery { getEpisode(any(), any(), any()) } returns episode.copy(number = episode.number)

        coEvery { getMediaLinks(tvShow = any(), any(), any()) } returns flowOf(LoadLinksState.Success)
        coEvery { getSeasonWithWatchProgress(any(), any()) } answers {
            val season = testTvShow.seasons.first().copy(episodes = listOf(episode))
            val episodes = listOf(
                EpisodeWithProgress(
                    episode = episode,
                    watchProgress = episodeProgress,
                ),
            )

            flowOf(
                Resource.Success(
                    SeasonWithProgress(
                        season = season,
                        episodes = episodes,
                    ),
                ),
            )
        }

        viewModel = PlayerScreenViewModel(
            appDispatchers = appDispatchers,
            cachedLinksRepository = cachedLinksRepository,
            getEpisode = getEpisode,
            getMediaLinks = getMediaLinks,
            getSeasonWithWatchProgress = getSeasonWithWatchProgress,
            providerRepository = providerRepository,
            setWatchProgress = setWatchProgress,
            userSessionManager = userSessionManager,
            watchProgressRepository = watchProgressRepository,
            player = player,
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `filmMetadata returns correct movie from nav args`() =
        runTest(testDispatcher) {
            createViewModelForMovie()

            expectThat(viewModel.filmMetadata).isEqualTo(testMovie)
        }

    @Test
    fun `filmMetadata returns correct tv show from nav args`() =
        runTest(testDispatcher) {
            createViewModelForTvShow()

            expectThat(viewModel.filmMetadata).isEqualTo(testTvShow)
        }

    @Test
    fun `providers returns enabled providers from repository`() =
        runTest(testDispatcher) {
            createViewModelForMovie()

            val providers = viewModel.providers

            expectThat(providers).isEqualTo(listOf(ProviderTestDefaults.getProviderMetadata(id = testProviderId)))
            verify { providerRepository.getEnabledProviders() }
        }

    @Test
    fun `uiState initializes with correct provider from cache`() =
        runTest(testDispatcher) {
            createViewModelForMovie()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.selectedProvider).isEqualTo(testProviderId)
                expectThat(state.loadLinksState).isEqualTo(LoadLinksState.Idle)
                expectThat(state.selectedSeason).isNull()
            }
        }

    @Test
    fun `selectedEpisode returns null for movie`() =
        runTest(testDispatcher) {
            createViewModelForMovie()

            viewModel.selectedEpisode.test {
                expectThat(awaitItem()).isNull()
            }
        }

    @Test
    fun `selectedEpisode returns episode for tv show`() =
        runTest(testDispatcher) {
            createViewModelForTvShow()

            viewModel.selectedEpisode.test {
                expectThat(awaitItem()).isEqualTo(testEpisode)
            }
        }

    @Test
    fun `watchProgress returns movie progress for movie`() =
        runTest(testDispatcher) {
            createViewModelForMovie()

            viewModel.watchProgress.test {
                val progress = awaitItem()
                expectThat(progress).isA<MovieProgress>().and {
                    get { filmId }.isEqualTo(testMovie.identifier)
                    get { ownerId }.isEqualTo(testUser.id)
                    get { this@get.progress }.isEqualTo(0L)
                }
            }
        }

    @Test
    fun `watchProgress returns episode progress for tv show`() =
        runTest(testDispatcher) {
            createViewModelForTvShow()

            viewModel.watchProgress.test {
                val progress = awaitItem()
                expectThat(progress).isA<EpisodeProgress>()

                expectThat(progress).isA<EpisodeProgress>().and {
                    get { filmId }.isEqualTo(testTvShow.identifier)
                    get { ownerId }.isEqualTo(testUser.id)
                    get { this@get.progress }.isEqualTo(0L)
                }
            }
        }

    @Test
    fun `onSeasonChange updates selected season in ui state`() =
        runTest(testDispatcher) {
            createViewModelForTvShow()

            viewModel.uiState.test {
                val initialState = awaitItem()
                expectThat(initialState.selectedSeason).isNull()

                viewModel.onSeasonChange(2)

                val updatedState = awaitItem()
                expectThat(updatedState.selectedSeason).isEqualTo(2)
            }
        }

    @Test
    fun `onAddSubtitle calls player addSubtitle`() =
        runTest(testDispatcher) {
            createViewModelForMovie()

            val subtitle = MediaSubtitle(
                label = "French",
                url = "https://example.com/french.srt",
                source = TrackSource.REMOTE,
            )

            viewModel.onAddSubtitle(subtitle)

            verify { player.addSubtitle(subtitle) }
        }

    @Test
    fun `onProviderChange switches media source when available`() =
        runTest(testDispatcher) {
            every { player.switchMediaSource(any()) } returns true
            createViewModelForMovie()

            advanceUntilIdle()

            val newProviderId = "provider2"
            viewModel.onProviderChange(newProviderId)

            advanceUntilIdle()

            verify { player.switchMediaSource(any()) }
        }

    @Test
    fun `onProviderChange loads links when media source not available`() =
        runTest(testDispatcher) {
            every { player.switchMediaSource(any()) } returns false
            every { cachedLinksRepository.getCache(any()) } returns testCachedLinks
            coEvery { getMediaLinks(movie = any<Movie>(), any()) } returns flowOf(LoadLinksState.Success)

            createViewModelForMovie()

            advanceUntilIdle()

            val newProviderId = "provider2"
            val cacheKey = CacheKey.create(
                filmId = testMovie.identifier,
                providerId = newProviderId,
                episode = null,
            )
            every { cachedLinksRepository.getCache(cacheKey) } returns testCachedLinks

            viewModel.onProviderChange(newProviderId)

            advanceUntilIdle()

            verify { player.prepare(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `seasonToDisplay emits correct season for tv show`() =
        runTest(testDispatcher) {
            createViewModelForTvShow()

            viewModel.onSeasonChange(1)
            advanceUntilIdle()

            viewModel.seasonToDisplay.test {
                skipItems(1) // Skip initial null value

                val emittedSeason = awaitItem()
                expectThat(emittedSeason)
                    .isNotNull()
                    .isA<Resource.Success<SeasonWithProgress>>()
                    .and {
                        get { data }.isNotNull().and {
                            get { season.number }.isEqualTo(1)
                            get { episodes.size }.isEqualTo(1)
                        }
                    }
            }
        }

    @Test
    fun `nextEpisode is set after successful link loading for tv show`() =
        runTest(testDispatcher) {
            val nextEpisode = FilmTestDefaults.getEpisode(
                id = "episode2",
                number = 2,
                season = 1,
                title = "Episode 2",
            )

            coEvery { getEpisode(any(), any(), any()) } returns nextEpisode
            every { cachedLinksRepository.getCache(any()) } returns testCachedLinks
            coEvery { getMediaLinks(any(), any<Episode>(), any()) } returns flowOf(LoadLinksState.Success)

            createViewModelForTvShow()

            advanceUntilIdle()

            expectThat(viewModel.nextEpisode).isNotNull()
        }

    @Test
    fun `loadLinks returns true when media source already loaded`() =
        runTest(testDispatcher) {
            every { player.switchMediaSource(any()) } returns true

            createViewModelForMovie()

            advanceUntilIdle()

            verify { player.switchMediaSource(any()) }
        }

    @Test
    fun `loadLinks prepares player with servers and subtitles from cache`() =
        runTest(testDispatcher) {
            every { player.switchMediaSource(any()) } returns false
            every { cachedLinksRepository.getCache(any()) } returns testCachedLinks
            coEvery { getMediaLinks(movie = any<Movie>(), any()) } returns flowOf(LoadLinksState.Success)

            createViewModelForMovie()
            advanceUntilIdle()

            verify {
                player.prepare(
                    key = any(),
                    servers = any(),
                    subtitles = any(),
                    startPositionMs = any(),
                    playImmediately = any(),
                )
            }
        }

    @Test
    fun `loadLinks handles error state correctly`() =
        runTest(testDispatcher) {
            val error = Exception("Network error")
            every { player.switchMediaSource(any()) } returns false
            every { cachedLinksRepository.getCache(any()) } returns null
            coEvery { getMediaLinks(movie = any<Movie>(), any()) } returns flowOf(LoadLinksState.Error(error))

            createViewModelForMovie()

            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.loadLinksState).isA<LoadLinksState.Error>()
            }
        }

    @Test
    fun `loadLinks for tv show requires episode`() =
        runTest(testDispatcher) {
            every { player.switchMediaSource(any()) } returns false
            every { cachedLinksRepository.getCache(any()) } returns null
            coEvery { getMediaLinks(any(), any<Episode>(), any()) } returns flowOf(LoadLinksState.Success)

            createViewModelForTvShow()
            advanceUntilIdle()

            coVerify { getMediaLinks(any(), any<Episode>(), any()) }
        }

    @Test
    fun `uiState updates selected provider after successful link loading`() =
        runTest(testDispatcher) {
            every { player.switchMediaSource(any()) } returns false
            every { cachedLinksRepository.getCache(any()) } returns testCachedLinks
            coEvery { getMediaLinks(movie = any<Movie>(), any()) } returns flowOf(LoadLinksState.Success)

            createViewModelForMovie()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.selectedProvider).isEqualTo(testProviderId)
                expectThat(state.loadLinksState).isEqualTo(LoadLinksState.Idle)
            }
        }

    @Test
    fun `cachedLinksRepository setCurrentCache is called after successful loading`() =
        runTest(testDispatcher) {
            every { player.switchMediaSource(any()) } returns false
            every { cachedLinksRepository.getCache(any()) } returns null
            coEvery { getMediaLinks(movie = any<Movie>(), any()) } returns flowOf(LoadLinksState.Success)

            createViewModelForMovie()
            advanceUntilIdle()

            verify { cachedLinksRepository.setCurrentCache(any()) }
        }
}
