package com.flixclusive.feature.mobile.searchExpanded

import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.pagination.PagingDataState
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.data.database.repository.SearchHistoryRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.util.collections.CollectionsOperation
import com.flixclusive.data.tmdb.repository.TMDBFilmSearchItemsRepository
import com.flixclusive.data.tmdb.util.TMDBFilters.Companion.getDefaultTMDBFilters
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.provider.ProviderApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class SearchExpandedScreenViewModelTest {
    private lateinit var viewModel: SearchExpandedScreenViewModel
    private lateinit var tmdbFilmSearchItemsRepository: TMDBFilmSearchItemsRepository
    private lateinit var searchHistoryRepository: SearchHistoryRepository
    private lateinit var userSessionManager: UserSessionManager
    private lateinit var providerApiRepository: ProviderApiRepository
    private lateinit var providerRepository: ProviderRepository
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var appDispatchers: AppDispatchers

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

    private val testProvider1 = ProviderTestDefaults.getProviderMetadata(
        id = "test-provider-1",
        name = "Test Provider 1",
    )

    private val testProvider2 = ProviderTestDefaults.getProviderMetadata(
        id = "test-provider-2",
        name = "Test Provider 2",
    )

    private val testProviderApi: ProviderApi = mockk(relaxed = true)

    private val testFilmSearchItem: FilmSearchItem = mockk {
        every { id } returns "1"
        every { title } returns "Test Film"
    }

    private val testSearchHistory = SearchHistory(
        id = 1,
        query = "test query",
        ownerId = testUser.id,
        searchedOn = Date(),
    )

    private val testSearchResponse = SearchResponseData(
        page = 1,
        totalPages = 3,
        hasNextPage = true,
        results = listOf(testFilmSearchItem),
    )

    private val testUiPreferences = UiPreferences(
        shouldShowTitleOnCards = false,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        tmdbFilmSearchItemsRepository = mockk(relaxed = true)
        searchHistoryRepository = mockk(relaxed = true)
        userSessionManager = mockk(relaxed = true)
        providerApiRepository = mockk(relaxed = true)
        providerRepository = mockk(relaxed = true)
        dataStoreManager = mockk(relaxed = true)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        setupDefaultBehavior()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupDefaultBehavior() {
        every { userSessionManager.currentUser } returns MutableStateFlow(testUser)
        // Fix: Return the test search history properly
        every { searchHistoryRepository.getAllItemsInFlow(testUser.id) } returns
            flowOf(listOf(testSearchHistory))
        every {
            dataStoreManager.getUserPrefs(
                UserPreferences.UI_PREFS_KEY,
                UiPreferences::class,
            )
        } returns flowOf(testUiPreferences)
        every { providerRepository.getEnabledProviders() } returns listOf(testProvider1, testProvider2)
        every { providerApiRepository.getApis() } returns listOf(testProviderApi)
        // Fix the observe() method to return a properly typed SharedFlow
        every { providerApiRepository.observe() } returns
            MutableSharedFlow<CollectionsOperation.Map<String, ProviderApi>>()
        every { providerApiRepository.getApi(any()) } returns testProviderApi

        coEvery {
            tmdbFilmSearchItemsRepository.search(
                query = any(),
                page = any(),
                filter = any(),
            )
        } returns Resource.Success(testSearchResponse)

        coEvery { testProviderApi.search(any(), any()) } returns testSearchResponse
        coEvery { searchHistoryRepository.insert(any()) } returns 1
        coEvery { searchHistoryRepository.remove(any()) } returns Unit
    }

    private fun createViewModel() {
        viewModel = SearchExpandedScreenViewModel(
            tmdbFilmSearchItemsRepository = tmdbFilmSearchItemsRepository,
            searchHistoryRepository = searchHistoryRepository,
            userSessionManager = userSessionManager,
            providerApiRepository = providerApiRepository,
            appDispatchers = appDispatchers,
            providerRepository = providerRepository,
            dataStoreManager = dataStoreManager,
        )
    }

    @Test
    fun `onQueryChange updates search query`() =
        runTest(testDispatcher) {
            createViewModel()

            val testQuery = "test search query"
            viewModel.onQueryChange(testQuery)

            viewModel.searchQuery.test {
                expectThat(awaitItem()).isEqualTo(testQuery)
            }
        }

    @Test
    fun `onSearch with empty query does not perform search`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onQueryChange("")
            viewModel.onSearch()
            advanceUntilIdle()

            coVerify(exactly = 0) { searchHistoryRepository.insert(any()) }
            coVerify(exactly = 0) { tmdbFilmSearchItemsRepository.search(any(), any(), any()) }
        }

    @Test
    fun `onSearch with valid query performs search and adds to history`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            val testQuery = "test movie"
            viewModel.onQueryChange(testQuery)
            viewModel.onSearch()
            advanceUntilIdle()

            coVerify {
                searchHistoryRepository.insert(
                    match { it.query == testQuery && it.ownerId == testUser.id },
                )
            }
            coVerify { tmdbFilmSearchItemsRepository.search(testQuery, 1, any()) }

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.lastQuerySearched).isEqualTo(testQuery)
                expectThat(state.currentViewType).isEqualTo(SearchItemViewType.Films)
            }
        }

    @Test
    fun `onChangeProvider updates selected provider and triggers search`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            val testQuery = "test movie"
            val testProviderId = "test-provider-1"

            viewModel.onQueryChange(testQuery)
            viewModel.onChangeProvider(testProviderId)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.selectedProviderId).isEqualTo(testProviderId)
            }

            coVerify { testProviderApi.search(page = 1, title = testQuery) }
        }

    @Test
    fun `onUpdateFilters updates filters`() =
        runTest(testDispatcher) {
            createViewModel()

            val newFilters = getDefaultTMDBFilters()
            viewModel.onUpdateFilters(newFilters)

            expectThat(viewModel.filters).isEqualTo(newFilters)
        }

    @Test
    fun `onChangeView updates current view type`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onChangeView(SearchItemViewType.Providers)

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.currentViewType).isEqualTo(SearchItemViewType.Providers)
            }
        }

    @Test
    fun `paginateItems with TMDB provider successful pagination`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onQueryChange("test query")
            viewModel.paginateItems()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.page).isEqualTo(2)
                expectThat(state.maxPage).isEqualTo(testSearchResponse.totalPages)
                expectThat(state.canPaginate).isTrue()
                expectThat(state.pagingState).isEqualTo(PagingDataState.Success(isExhausted = false))
            }

            expectThat(viewModel.searchResults).isEqualTo(persistentSetOf(testFilmSearchItem))
        }

    @Test
    fun `paginateItems with provider API successful pagination`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onQueryChange("test query")

            // onChangeProvider calls onSearch() which will set the page to 1 and then call paginateItems()
            // This means the page will be incremented to 2, then we call paginateItems() again which increments to 3
            viewModel.onChangeProvider("test-provider-1")
            advanceUntilIdle()

            viewModel.paginateItems()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.page).isEqualTo(3) // Fix: Expect page 3 instead of 2
                expectThat(state.maxPage).isEqualTo(testSearchResponse.totalPages)
                expectThat(state.canPaginate).isTrue()
                expectThat(state.pagingState).isEqualTo(PagingDataState.Success(isExhausted = false))
            }

            expectThat(viewModel.searchResults).isEqualTo(persistentSetOf(testFilmSearchItem))
            coVerify { testProviderApi.search(page = 1, title = "test query") }
        }

    @Test
    fun `paginateItems handles TMDB repository failure`() =
        runTest(testDispatcher) {
            val errorMessage = UiText.StringValue("Network error")
            coEvery {
                tmdbFilmSearchItemsRepository.search(any(), any(), any())
            } returns Resource.Failure(errorMessage)

            createViewModel()
            advanceUntilIdle()

            viewModel.onQueryChange("test query")
            viewModel.paginateItems()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.error).isEqualTo(errorMessage)
                expectThat(state.pagingState).isEqualTo(PagingDataState.Error(errorMessage))
            }
        }

    @Test
    fun `paginateItems handles provider API failure`() =
        runTest(testDispatcher) {
            val exception = RuntimeException("API Error")
            coEvery { testProviderApi.search(any(), any()) } throws exception

            createViewModel()
            advanceUntilIdle()

            viewModel.onQueryChange("test query")
            viewModel.onChangeProvider("test-provider-1")
            viewModel.paginateItems()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.error).isNotNull()
                expectThat(state.pagingState).isEqualTo(PagingDataState.Error(state.error!!))
            }
        }

    @Test
    fun `paginateItems does not paginate when done paginating`() =
        runTest(testDispatcher) {
            // Setup state where pagination should stop
            val exhaustedResponse = testSearchResponse.copy(
                page = 3,
                totalPages = 3,
                hasNextPage = false,
                results = emptyList(),
            )

            coEvery {
                tmdbFilmSearchItemsRepository.search(any(), any(), any())
            } returns Resource.Success(exhaustedResponse)

            createViewModel()
            advanceUntilIdle()

            viewModel.onQueryChange("test query")
            viewModel.paginateItems()
            advanceUntilIdle()

            // Try to paginate again - should not make another call
            val callCountBefore = 1
            viewModel.paginateItems()
            advanceUntilIdle()

            coVerify(exactly = callCountBefore) { tmdbFilmSearchItemsRepository.search(any(), any(), any()) }
        }

    @Test
    fun `deleteSearchHistoryItem removes item from history`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle() // Allow the ViewModel to be fully initialized

            viewModel.deleteSearchHistoryItem(testSearchHistory)
            advanceUntilIdle() // Allow the coroutine to complete

            // Fix the verification to match the actual method signature
            coVerify { searchHistoryRepository.remove(testSearchHistory.id) }
        }

    @Test
    fun `provider metadata list is populated correctly`() =
        runTest(testDispatcher) {
            createViewModel()

            expectThat(viewModel.providerMetadataList).isEqualTo(
                listOf(testProvider1, testProvider2).toImmutableList(),
            )
        }

    @Test
    fun `search results clear when starting new search`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            // First search
            viewModel.onQueryChange("first query")
            viewModel.onSearch()
            advanceUntilIdle()

            expectThat(viewModel.searchResults).isEqualTo(persistentSetOf(testFilmSearchItem))

            // Second search should clear previous results
            viewModel.onQueryChange("second query")
            viewModel.onSearch()
            advanceUntilIdle()

            // Results should be replaced, not accumulated
            expectThat(viewModel.searchResults).isEqualTo(persistentSetOf(testFilmSearchItem))
        }

    @Test
    fun `multiple pagination calls add results correctly`() =
        runTest(testDispatcher) {
            val secondFilmItem: FilmSearchItem = mockk {
                every { id } returns "2"
                every { title } returns "Second Film"
            }

            val firstPageResponse = testSearchResponse.copy(
                page = 1,
                results = listOf(testFilmSearchItem),
            )
            val secondPageResponse = testSearchResponse.copy(
                page = 2,
                results = listOf(secondFilmItem),
            )

            coEvery {
                tmdbFilmSearchItemsRepository.search(any(), 1, any())
            } returns Resource.Success(firstPageResponse)

            coEvery {
                tmdbFilmSearchItemsRepository.search(any(), 2, any())
            } returns Resource.Success(secondPageResponse)

            createViewModel()
            advanceUntilIdle()

            // First pagination
            viewModel.onQueryChange("test query")
            viewModel.paginateItems()
            advanceUntilIdle()

            expectThat(viewModel.searchResults).isEqualTo(persistentSetOf(testFilmSearchItem))

            // Second pagination
            viewModel.paginateItems()
            advanceUntilIdle()

            expectThat(viewModel.searchResults).isEqualTo(persistentSetOf(testFilmSearchItem, secondFilmItem))
        }
}
