package com.flixclusive.feature.mobile.film

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.navigation.navargs.FilmScreenNavArgs
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.domain.database.usecase.ToggleWatchlistStatusUseCase
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonUseCase
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.film.util.FilmType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue
import java.util.Date
import com.flixclusive.core.strings.R as LocaleR

class FilmScreenViewModelTest {
    private lateinit var viewModel: FilmScreenViewModel
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var savedStateHandle: SavedStateHandle

    // Mock dependencies
    private val watchProgressRepository = mockk<WatchProgressRepository>()
    private val getSeason = mockk<GetSeasonUseCase>()
    private val toggleWatchlistStatus = mockk<ToggleWatchlistStatusUseCase>()
    private val dataStoreManager = mockk<DataStoreManager>()
    private val userSessionManager = mockk<UserSessionManager>()
    private val getFilmMetadata = mockk<GetFilmMetadataUseCase>()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testUser = User(id = 1, name = "Test User", image = 0)

    private val testMovie = Movie(
        id = "movie-1",
        title = "Test Movie",
        posterImage = "poster.jpg",
        homePage = "https://example.com",
        providerId = DEFAULT_FILM_SOURCE_NAME,
        backdropImage = "backdrop.jpg",
        rating = 8.5,
        overview = "A test movie",
        year = 2023,
        runtime = 120,
    )

    private val testTvShow = TvShow(
        id = "tv-1",
        title = "Test TV Show",
        posterImage = "poster.jpg",
        homePage = "https://example.com",
        providerId = DEFAULT_FILM_SOURCE_NAME,
        backdropImage = "backdrop.jpg",
        rating = 9.0,
        overview = "A test TV show",
        year = 2023,
        totalSeasons = 3,
        totalEpisodes = 30,
        seasons = listOf(
            Season(
                number = 1,
                name = "Season 1",
                overview = "First season",
                episodes = listOf(
                    Episode(
                        id = "ep-1",
                        title = "Episode 1",
                        number = 1,
                        season = 1,
                        overview = "First episode",
                    ),
                ),
            ),
        ),
    )

    private val testSeason = Season(
        number = 2,
        name = "Season 2",
        overview = "Second season",
        episodes = listOf(
            Episode(
                id = "ep-2-1",
                title = "Episode 1",
                number = 1,
                season = 2,
                overview = "First episode of season 2",
            ),
        ),
    )

    private val testMovieProgress = MovieProgressWithMetadata(
        watchData = MovieProgress(
            id = 1L,
            filmId = "movie-1",
            ownerId = 1,
            progress = 60000L,
            status = WatchStatus.WATCHING,
            duration = 120000L,
            watchedAt = Date(),
            watchCount = 1,
        ),
        film = DBFilm(
            id = "movie-1",
            title = "Test Movie",
            filmType = FilmType.MOVIE,
            posterImage = "poster.jpg",
            tmdbId = 123,
        ),
    )

    private val testEpisodeProgress = EpisodeProgressWithMetadata(
        watchData = EpisodeProgress(
            id = 1L,
            filmId = "tv-1",
            ownerId = 1,
            progress = 1500000L,
            duration = 3000000L,
            status = WatchStatus.WATCHING,
            watchedAt = Date(),
            seasonNumber = 1,
            episodeNumber = 1,
        ),
        film = DBFilm(
            id = "tv-1",
            title = "Test TV Show",
            filmType = FilmType.TV_SHOW,
            posterImage = "poster.jpg",
            tmdbId = 456,
        ),
    )

    private val testFilmSearchItem = FilmSearchItem(
        id = "search-1",
        providerId = DEFAULT_FILM_SOURCE_NAME,
        filmType = FilmType.MOVIE,
        homePage = "https://example.com",
        title = "Search Movie",
        posterImage = "poster.jpg",
        rating = 7.5,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        appDispatchers = object : AppDispatchers {
            override val default = testDispatcher
            override val io = testDispatcher
            override val main = testDispatcher
            override val unconfined = testDispatcher
            override val ioScope get() = TestScope(testDispatcher)
            override val defaultScope get() = TestScope(testDispatcher)
            override val mainScope get() = TestScope(testDispatcher)
        }

        // Setup default mock behaviors
        every { userSessionManager.currentUser } returns MutableStateFlow(testUser)
        every { dataStoreManager.getUserPrefs(UserPreferences.UI_PREFS_KEY, UiPreferences::class) } returns
            flowOf(UiPreferences(shouldShowTitleOnCards = false))
        every { watchProgressRepository.getAsFlow(any(), any(), any()) } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModelWithMovie(): FilmScreenViewModel {
        savedStateHandle = SavedStateHandle().apply {
            set("film", FilmScreenNavArgs(testMovie))
        }

        return FilmScreenViewModel(
            watchProgressRepository = watchProgressRepository,
            getSeason = getSeason,
            toggleWatchlistStatus = toggleWatchlistStatus,
            dataStoreManager = dataStoreManager,
            userSessionManager = userSessionManager,
            savedStateHandle = savedStateHandle,
            appDispatchers = appDispatchers,
            getFilmMetadata = getFilmMetadata,
        )
    }

    private fun createViewModelWithTvShow(): FilmScreenViewModel {
        savedStateHandle = SavedStateHandle().apply {
            set("film", FilmScreenNavArgs(testTvShow))
        }

        return FilmScreenViewModel(
            watchProgressRepository = watchProgressRepository,
            getSeason = getSeason,
            toggleWatchlistStatus = toggleWatchlistStatus,
            dataStoreManager = dataStoreManager,
            userSessionManager = userSessionManager,
            savedStateHandle = savedStateHandle,
            appDispatchers = appDispatchers,
            getFilmMetadata = getFilmMetadata,
        )
    }

    private fun createViewModelWithFilmSearchItem(): FilmScreenViewModel {
        savedStateHandle = SavedStateHandle().apply {
            set("film", FilmScreenNavArgs(testFilmSearchItem))
        }

        return FilmScreenViewModel(
            watchProgressRepository = watchProgressRepository,
            getSeason = getSeason,
            toggleWatchlistStatus = toggleWatchlistStatus,
            dataStoreManager = dataStoreManager,
            userSessionManager = userSessionManager,
            savedStateHandle = savedStateHandle,
            appDispatchers = appDispatchers,
            getFilmMetadata = getFilmMetadata,
        )
    }

    @Test
    fun `initial state is correct when created with movie metadata`() =
        runTest(testDispatcher) {
            viewModel = createViewModelWithMovie()

            expectThat(viewModel.uiState.value) {
                get { isLoading }.isFalse()
                get { error }.isNull()
                get { selectedSeason }.isNull()
            }

            expectThat(viewModel.metadata.value).isEqualTo(testMovie)
        }

    @Test
    fun `initial state is correct when created with tv show metadata`() =
        runTest(testDispatcher) {
            viewModel = createViewModelWithTvShow()

            expectThat(viewModel.uiState.value) {
                get { isLoading }.isFalse()
                get { error }.isNull()
                get { selectedSeason }.isEqualTo(testTvShow.totalSeasons)
            }

            expectThat(viewModel.metadata.value).isEqualTo(testTvShow)
        }

    @Test
    fun `fetches metadata successfully when film is not metadata`() =
        runTest(testDispatcher) {
            coEvery { getFilmMetadata(any()) } returns Resource.Success(testMovie)

            viewModel = createViewModelWithFilmSearchItem()
            advanceUntilIdle()

            expectThat(viewModel.uiState.value) {
                get { isLoading }.isFalse()
                get { error }.isNull()
            }

            expectThat(viewModel.metadata.value).isEqualTo(testMovie)
            coVerify { getFilmMetadata(any()) }
        }

    @Test
    fun `shows error when metadata fetch fails`() =
        runTest(testDispatcher) {
            val errorMessage = UiText.from(LocaleR.string.error_film_message)
            coEvery { getFilmMetadata(any()) } returns Resource.Failure(errorMessage)

            viewModel = createViewModelWithFilmSearchItem()
            advanceUntilIdle()

            expectThat(viewModel.uiState.value) {
                get { isLoading }.isFalse()
                get { error }.isEqualTo(errorMessage)
            }

            expectThat(viewModel.metadata.value).isNull()
        }

    @Test
    fun `shows loading state during metadata fetch`() =
        runTest(testDispatcher) {
            coEvery { getFilmMetadata(any()) } returns Resource.Loading

            viewModel = createViewModelWithFilmSearchItem()

            expectThat(viewModel.uiState.value) {
                get { isLoading }.isTrue()
                get { error }.isNull()
            }
        }

    @Test
    fun `sets initial selected season from watch progress for tv show`() =
        runTest(testDispatcher) {
            every { watchProgressRepository.getAsFlow(any(), any(), any()) } returns
                flowOf(testEpisodeProgress)

            viewModel = createViewModelWithTvShow()
            advanceUntilIdle()

            expectThat(viewModel.uiState.value) {
                get { selectedSeason }.isEqualTo(testEpisodeProgress.watchData.seasonNumber)
            }
        }

    @Test
    fun `uses total seasons as initial selected season when no watch progress exists`() =
        runTest(testDispatcher) {
            every { watchProgressRepository.getAsFlow(any(), any(), any()) } returns flowOf(null)

            viewModel = createViewModelWithTvShow()
            advanceUntilIdle()

            expectThat(viewModel.uiState.value) {
                get { selectedSeason }.isEqualTo(testTvShow.totalSeasons)
            }
        }

    @Test
    fun `onSeasonChange updates selected season`() =
        runTest(testDispatcher) {
            viewModel = createViewModelWithTvShow()

            viewModel.onSeasonChange(2)

            expectThat(viewModel.uiState.value) {
                get { selectedSeason }.isEqualTo(2)
            }
        }

    @Test
    fun `onRetry fetches metadata again after error`() =
        runTest(testDispatcher) {
            coEvery { getFilmMetadata(any()) } returns Resource.Failure(UiText.from("Error"))

            viewModel = createViewModelWithFilmSearchItem()
            advanceUntilIdle()

            expectThat(viewModel.uiState.value.error).isA<UiText>()

            coEvery { getFilmMetadata(any()) } returns Resource.Success(testMovie)

            viewModel.onRetry()
            advanceUntilIdle()

            expectThat(viewModel.uiState.value) {
                get { error }.isNull()
                get { isLoading }.isFalse()
            }
            expectThat(viewModel.metadata.value).isEqualTo(testMovie)
        }

    @Test
    fun `onConsumeError clears error state`() =
        runTest(testDispatcher) {
            coEvery { getFilmMetadata(any()) } returns Resource.Failure(UiText.from("Error"))

            viewModel = createViewModelWithFilmSearchItem()
            advanceUntilIdle()

            expectThat(viewModel.uiState.value.error).isA<UiText>()

            viewModel.onConsumeError()

            expectThat(viewModel.uiState.value.error).isNull()
        }

    @Test
    fun `showFilmTitles reflects datastore preference`() =
        runTest(testDispatcher) {
            every { dataStoreManager.getUserPrefs(UserPreferences.UI_PREFS_KEY, UiPreferences::class) } returns
                flowOf(UiPreferences(shouldShowTitleOnCards = true))

            viewModel = createViewModelWithMovie()

            viewModel.showFilmTitles.test {
                expectThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `watchProgress returns current user watch progress`() =
        runTest(testDispatcher) {
            every { watchProgressRepository.getAsFlow(any(), any(), any()) } returns
                flowOf(testMovieProgress)

            viewModel = createViewModelWithMovie()

            viewModel.watchProgress.test {
                expectThat(awaitItem()).isEqualTo(testMovieProgress)
            }
        }

    @Test
    fun `seasonToDisplay fetches season when tv show and season selected`() =
        runTest(testDispatcher) {
            every { getSeason(any(), any()) } returns flowOf(Resource.Success(testSeason))

            viewModel = createViewModelWithTvShow()

            viewModel.onSeasonChange(2)
            advanceUntilIdle()

            viewModel.seasonToDisplay.test {
                val result = awaitItem()
                expectThat(result).isA<Resource.Success<Season>>()
                expectThat((result as Resource.Success).data).isEqualTo(testSeason)
            }
        }

    @Test
    fun `seasonToDisplay returns null for movie`() =
        runTest(testDispatcher) {
            viewModel = createViewModelWithMovie()

            viewModel.seasonToDisplay.test {
                expectThat(awaitItem()).isNull()
            }
        }

    @Test
    fun `metadata is updated when new season is fetched`() =
        runTest(testDispatcher) {
            every { getSeason(any(), any()) } returns flowOf(Resource.Success(testSeason))

            viewModel = createViewModelWithTvShow()

            viewModel.onSeasonChange(2)
            advanceUntilIdle()

            expectThat(viewModel.metadata.value).isA<TvShow>()
            val updatedTvShow = viewModel.metadata.value as TvShow
            expectThat(updatedTvShow.seasons).contains(testSeason)
        }
}
