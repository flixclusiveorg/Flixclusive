package com.flixclusive.feature.mobile.settings.screen.root

import app.cash.turbine.test
import com.flixclusive.core.common.config.BuildConfigProvider
import com.flixclusive.core.common.config.BuildType
import com.flixclusive.core.common.config.CustomBuildConfig
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.data.database.repository.SearchHistoryRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.CacheKey
import com.flixclusive.data.provider.repository.CachedLinks
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private lateinit var viewModel: SettingsViewModel
    private lateinit var userSessionManager: UserSessionManager
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var searchHistoryRepository: SearchHistoryRepository
    private lateinit var providerRepository: ProviderRepository
    private lateinit var unloadProviderUseCase: UnloadProviderUseCase
    private lateinit var cachedLinksRepository: CachedLinksRepository
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var buildConfigProvider: BuildConfigProvider

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

    private val testBuildConfig = CustomBuildConfig(
        versionName = "1.0.0",
        versionCode = 10000,
        commitHash = "abc123",
        buildType = BuildType.DEBUG,
        applicationId = "com.flixclusive.test",
        applicationName = "Flixclusive Test",
    )

    private val testSystemPreferences = SystemPreferences()

    private val testSearchHistory = listOf(
        SearchHistory(
            id = 1,
            query = "test query 1",
            ownerId = testUser.id,
            searchedOn = Date(),
        ),
        SearchHistory(
            id = 2,
            query = "test query 2",
            ownerId = testUser.id,
            searchedOn = Date(),
        ),
    )

    private val testProviderMetadata1 = ProviderTestDefaults.getProviderMetadata(
        id = "provider-1",
        name = "Test Provider 1",
    )

    private val testProviderMetadata2 = ProviderTestDefaults.getProviderMetadata(
        id = "provider-2",
        name = "Test Provider 2",
    )

    private val testCachedLinks = mapOf(
        CacheKey.create(
            filmId = "film1",
            providerId = testProviderMetadata1.id,
            episode = null,
        ) to CachedLinks(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        userSessionManager = mockk(relaxed = true)
        dataStoreManager = mockk(relaxed = true)
        searchHistoryRepository = mockk(relaxed = true)
        providerRepository = mockk(relaxed = true)
        unloadProviderUseCase = mockk(relaxed = true)
        cachedLinksRepository = mockk(relaxed = true)
        buildConfigProvider = mockk(relaxed = true)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        setupDefaultBehavior()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupDefaultBehavior() {
        every { userSessionManager.currentUser } returns MutableStateFlow(testUser)
        every { buildConfigProvider.get() } returns testBuildConfig
        every { dataStoreManager.getSystemPrefs() } returns flowOf(testSystemPreferences)
        every { searchHistoryRepository.getAllItemsInFlow(testUser.id) } returns flowOf(testSearchHistory)
        every { cachedLinksRepository.caches } returns MutableStateFlow(testCachedLinks)
        every { providerRepository.getProviders() } returns listOf(testProviderMetadata1, testProviderMetadata2)

        coEvery { dataStoreManager.updateSystemPrefs(any()) } returns Unit
        coEvery { dataStoreManager.updateUserPrefs<UiPreferences>(any(), any(), any()) } returns Unit
        coEvery { searchHistoryRepository.clearAll(any()) } returns Unit
        coEvery { cachedLinksRepository.clear() } returns Unit
        coEvery { unloadProviderUseCase(any(), any()) } returns Unit
    }

    private fun createViewModel() {
        viewModel = SettingsViewModel(
            userSessionManager = userSessionManager,
            dataStoreManager = dataStoreManager,
            searchHistoryRepository = searchHistoryRepository,
            providerRepository = providerRepository,
            unloadProviderUseCase = unloadProviderUseCase,
            cachedLinksRepository = cachedLinksRepository,
            appDispatchers = appDispatchers,
            _buildConfig = buildConfigProvider,
        )
    }

    @Test
    fun `currentUser returns user from session manager`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.currentUser.test {
                expectThat(awaitItem()).isEqualTo(testUser)
            }
        }

    @Test
    fun `buildConfig returns correct build configuration`() =
        runTest(testDispatcher) {
            createViewModel()

            val result = viewModel.buildConfig

            expectThat(result).isEqualTo(testBuildConfig)
            verify { buildConfigProvider.get() }
        }

    @Test
    fun `searchHistoryCount returns correct count from repository`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.searchHistoryCount.test {
                skipItems(1) // Skip initial value emitted by StateFlow

                expectThat(awaitItem()).isEqualTo(testSearchHistory.size)
            }
        }

    @Test
    fun `cachedLinksSize returns correct cache size`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.cachedLinksSize.test {
                skipItems(1) // Skip initial value emitted by StateFlow

                expectThat(awaitItem()).isEqualTo(testCachedLinks.size)
            }
        }

    @Test
    fun `systemPreferences returns system preferences from datastore`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.systemPreferences.test {
                expectThat(awaitItem()).isEqualTo(testSystemPreferences)
            }
        }

    @Test
    fun `getUserPrefsAsState returns user preferences as state flow`() =
        runTest(testDispatcher) {
            val testUiPreferences = UiPreferences(shouldShowTitleOnCards = true)
            every {
                dataStoreManager.getUserPrefs<UiPreferences>(
                    UserPreferences.UI_PREFS_KEY,
                    UiPreferences::class,
                )
            } returns flowOf(testUiPreferences)

            createViewModel()

            val userPrefsFlow = viewModel.getUserPrefsAsState<UiPreferences>(UserPreferences.UI_PREFS_KEY)
            userPrefsFlow.test {
                skipItems(1) // Skip initial value emitted by StateFlow

                expectThat(awaitItem()).isEqualTo(testUiPreferences)
            }
        }

    @Test
    fun `updateSystemPrefs calls datastore manager and returns true`() =
        runTest(testDispatcher) {
            createViewModel()

            val transform: suspend (SystemPreferences) -> SystemPreferences = { it.copy() }
            val result = viewModel.updateSystemPrefs(transform)

            expectThat(result).isTrue()
            coVerify { dataStoreManager.updateSystemPrefs(transform) }
        }

    @Test
    fun `updateUserPrefs calls datastore manager and returns true`() =
        runTest(testDispatcher) {
            createViewModel()

            val transform: suspend (UiPreferences) -> UiPreferences = { it.copy() }
            val result = viewModel.updateUserPrefs<UiPreferences>(
                UserPreferences.UI_PREFS_KEY,
                transform,
            )

            expectThat(result).isTrue()
            coVerify {
                dataStoreManager.updateUserPrefs<UiPreferences>(
                    UserPreferences.UI_PREFS_KEY,
                    UiPreferences::class,
                    transform,
                )
            }
        }

    @Test
    fun `clearSearchHistory clears all search history for current user`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.clearSearchHistory()
            advanceUntilIdle()

            coVerify { searchHistoryRepository.clearAll(testUser.id) }
        }

    @Test
    fun `clearCacheLinks calls repository clear method`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.clearCacheLinks()

            verify { cachedLinksRepository.clear() }
        }

    @Test
    fun `deleteRepositories updates provider preferences with empty repositories`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.deleteRepositories()
            advanceUntilIdle()

            coVerify {
                dataStoreManager.updateUserPrefs<ProviderPreferences>(
                    UserPreferences.PROVIDER_PREFS_KEY,
                    ProviderPreferences::class,
                    any(),
                )
            }
        }

    @Test
    fun `deleteProviders unloads all providers from repository`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.deleteProviders()
            advanceUntilIdle()

            coVerify { unloadProviderUseCase(testProviderMetadata1, any()) }
            coVerify { unloadProviderUseCase(testProviderMetadata2, any()) }
        }

    @Test
    fun `searchHistoryCount updates when user changes`() =
        runTest(testDispatcher) {
            val newUser = User(
                id = 2,
                name = "New User",
                image = 1,
                pin = null,
                pinHint = null,
            )

            val newUserSearchHistory = listOf(
                SearchHistory(
                    id = 3,
                    query = "new user query",
                    ownerId = newUser.id,
                    searchedOn = Date(),
                ),
            )

            val userStateFlow = MutableStateFlow(testUser)
            every { userSessionManager.currentUser } returns userStateFlow
            every { searchHistoryRepository.getAllItemsInFlow(newUser.id) } returns flowOf(newUserSearchHistory)

            createViewModel()

            viewModel.searchHistoryCount.test {
                skipItems(1) // Skip initial value emitted by StateFlow

                // Initial count for testUser
                expectThat(awaitItem()).isEqualTo(testSearchHistory.size)

                // Change user
                userStateFlow.value = newUser

                // Count should update for new user
                expectThat(awaitItem()).isEqualTo(newUserSearchHistory.size)
            }
        }

    @Test
    fun `cachedLinksSize updates when cache changes`() =
        runTest(testDispatcher) {
            val initialCacheFlow = MutableStateFlow(testCachedLinks)
            every { cachedLinksRepository.caches } returns initialCacheFlow

            createViewModel()

            viewModel.cachedLinksSize.test {
                skipItems(1) // Skip initial value emitted by StateFlow

                // Initial cache size
                expectThat(awaitItem()).isEqualTo(testCachedLinks.size)

                // Change cache size
                val newCache = mapOf(
                    CacheKey.create(
                        filmId = "film1",
                        providerId = testProviderMetadata1.id,
                        episode = null,
                    ) to CachedLinks(),
                    CacheKey.create(
                        filmId = "film2",
                        providerId = testProviderMetadata2.id,
                        episode = null,
                    ) to CachedLinks(),
                )
                initialCacheFlow.value = newCache

                // Size should update
                expectThat(awaitItem()).isEqualTo(newCache.size)
            }
        }

    @Test
    fun `systemPreferences updates when preferences change`() =
        runTest(testDispatcher) {
            val newSystemPreferences = testSystemPreferences.copy(isFirstTimeUserLaunch = false)
            val preferencesFlow = MutableStateFlow(testSystemPreferences)
            every { dataStoreManager.getSystemPrefs() } returns preferencesFlow

            createViewModel()
            advanceUntilIdle()

            viewModel.systemPreferences.test {
                // Initial preferences
                expectThat(awaitItem()).isEqualTo(testSystemPreferences)

                // Change preferences
                preferencesFlow.value = newSystemPreferences

                // Should receive updated preferences
                expectThat(awaitItem()).isEqualTo(newSystemPreferences)
            }
        }

    @Test
    fun `getUserPrefsAsState creates new instance when no preferences exist`() =
        runTest(testDispatcher) {
            every {
                dataStoreManager.getUserPrefs<UiPreferences>(any(), any())
            } returns flowOf(UiPreferences()) // Default instance

            createViewModel()

            val userPrefsFlow = viewModel.getUserPrefsAsState<UiPreferences>(UserPreferences.UI_PREFS_KEY)
            userPrefsFlow.test {
                val result = awaitItem()
                expectThat(result).isEqualTo(UiPreferences()) // Should be default instance
            }
        }
}
