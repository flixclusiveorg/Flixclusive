package com.flixclusive.feature.mobile.repository.manage

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.domain.provider.usecase.get.GetRepositoryUseCase
import com.flixclusive.model.provider.Repository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue
import kotlin.time.Duration.Companion.seconds

class RepositoryManagerViewModelTest {
    private lateinit var getRepository: GetRepositoryUseCase
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var viewModel: RepositoryManagerViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val providerPreferencesFlow = MutableStateFlow(
        ProviderPreferences(
            repositories = emptyList(),
        ),
    )

    private val dummyRepository1 = Repository(
        owner = "owner1",
        name = "repo1",
        url = "https://github.com/owner1/repo1",
        rawLinkFormat = "https://raw.githubusercontent.com/owner1/repo1/%branch%/%filename%",
    )

    private val dummyRepository2 = Repository(
        owner = "owner2",
        name = "repo2",
        url = "https://github.com/owner2/repo2",
        rawLinkFormat = "https://raw.githubusercontent.com/owner2/repo2/%branch%/%filename%",
    )

    private val dummyRepository3 = Repository(
        owner = "testowner",
        name = "testrepo",
        url = "https://github.com/testowner/testrepo",
        rawLinkFormat = "https://raw.githubusercontent.com/testowner/testrepo/%branch%/%filename%",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getRepository = mockk(relaxed = true)
        dataStoreManager = mockk(relaxed = true)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        every {
            dataStoreManager.getUserPrefs(
                UserPreferences.PROVIDER_PREFS_KEY,
                ProviderPreferences::class,
            )
        } returns providerPreferencesFlow

        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(
                UserPreferences.PROVIDER_PREFS_KEY,
                ProviderPreferences::class,
                any(),
            )
        } coAnswers {
            val transform = thirdArg<suspend (ProviderPreferences) -> ProviderPreferences>()
            val newPrefs = transform(providerPreferencesFlow.value)
            providerPreferencesFlow.value = newPrefs
        }

        viewModel = RepositoryManagerViewModel(
            getRepository = getRepository,
            dataStoreManager = dataStoreManager,
            appDispatchers = appDispatchers,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `repositories flow should emit datastore repositories`() =
        runTest(testDispatcher) {
            val repositories = listOf(dummyRepository1, dummyRepository2)
            providerPreferencesFlow.value = ProviderPreferences(repositories = repositories)

            advanceUntilIdle() // Ensure all flows have emitted

            viewModel.repositories.test {
                expectThat(awaitItem()).isEqualTo(repositories)
            }
        }

    @Test
    fun `onSearchQueryChange should update search query`() =
        runTest(testDispatcher) {
            val query = "test query"

            viewModel.onSearchQueryChange(query)

            viewModel.searchQuery.test {
                expectThat(awaitItem()).isEqualTo(query)
            }
        }

    @Test
    fun `onUrlQueryChange should update url query`() =
        runTest(testDispatcher) {
            val url = "https://github.com/test/repo"

            viewModel.onUrlQueryChange(url)

            viewModel.urlQuery.test {
                expectThat(awaitItem()).isEqualTo(url)
            }
        }

    @Test
    fun `onToggleSearchBar should update search bar visibility`() =
        runTest(testDispatcher) {
            turbineScope {
                val uiStateTurbine = viewModel.uiState.testIn(this)
                uiStateTurbine.skipItems(1) // Skip initial state

                viewModel.onToggleSearchBar(true)
                expectThat(uiStateTurbine.awaitItem().isShowingSearchBar).isTrue()

                viewModel.onToggleSearchBar(false)
                expectThat(uiStateTurbine.awaitItem().isShowingSearchBar).isFalse()

                uiStateTurbine.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `toggleRepositorySelection should add repository when not selected`() =
        runTest(testDispatcher) {
            viewModel.toggleRepositorySelection(dummyRepository1)

            viewModel.selectedRepositories.test {
                val selection = awaitItem()
                expectThat(selection).contains(dummyRepository1)
                expectThat(selection).hasSize(1)
            }
        }

    @Test
    fun `toggleRepositorySelection should remove repository when already selected`() =
        runTest(testDispatcher) {
            viewModel.toggleRepositorySelection(dummyRepository1)
            viewModel.toggleRepositorySelection(dummyRepository1)

            viewModel.selectedRepositories.test {
                expectThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun `clearSelection should clear all selected repositories`() =
        runTest(testDispatcher) {
            viewModel.toggleRepositorySelection(dummyRepository1)
            viewModel.toggleRepositorySelection(dummyRepository2)
            viewModel.clearSelection()

            viewModel.selectedRepositories.test {
                expectThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun `searchResults should filter repositories by name`() =
        runTest(testDispatcher) {
            val repositories = listOf(dummyRepository1, dummyRepository2, dummyRepository3)
            providerPreferencesFlow.value = ProviderPreferences(repositories = repositories)

            viewModel.onSearchQueryChange("repo1")
            advanceUntilIdle()

            viewModel.searchResults.test {
                skipItems(1) // Skip initial empty emission
                val results = awaitItem()
                expectThat(results).hasSize(1)
                expectThat(results).contains(dummyRepository1)
            }
        }

    @Test
    fun `searchResults should filter repositories by owner`() =
        runTest(testDispatcher) {
            val repositories = listOf(dummyRepository1, dummyRepository2, dummyRepository3)
            providerPreferencesFlow.value = ProviderPreferences(repositories = repositories)

            viewModel.onSearchQueryChange("testowner")
            advanceUntilIdle()

            viewModel.searchResults.test {
                skipItems(1) // Skip initial empty emission
                val results = awaitItem()
                expectThat(results).hasSize(1)
                expectThat(results).contains(dummyRepository3)
            }
        }

    @Test
    fun `searchResults should return all repositories when query is blank`() =
        runTest(testDispatcher) {
            val repositories = listOf(dummyRepository1, dummyRepository2, dummyRepository3)
            providerPreferencesFlow.value = ProviderPreferences(repositories = repositories)

            viewModel.onSearchQueryChange("")
            advanceUntilIdle()

            viewModel.searchResults.test {
                skipItems(1) // Skip initial empty emission
                expectThat(awaitItem()).isEqualTo(repositories)
            }
        }

    @Test
    fun `onAddLink should add repository on success`() =
        runTest(testDispatcher) {
            val url = "https://github.com/test/repo"
            coEvery { getRepository(url) } returns Resource.Success(dummyRepository1)

            viewModel.onUrlQueryChange(url)
            viewModel.onAddLink()
            advanceUntilIdle()

            viewModel.repositories.test {
                val repositories = awaitItem()
                expectThat(repositories).contains(dummyRepository1)
            }

            coVerify { getRepository(url) }
        }

    @Test
    fun `onAddLink should set error on failure`() =
        runTest(testDispatcher) {
            val url = "https://github.com/test/repo"
            val errorMessage = UiText.StringResource(123)
            coEvery { getRepository(url) } returns Resource.Failure(errorMessage)

            viewModel.onUrlQueryChange(url)
            viewModel.onAddLink()
            advanceUntilIdle()

            viewModel.uiState.test {
                expectThat(awaitItem().error).isEqualTo(errorMessage)
            }
        }

    @Test
    fun `onAddLink should not start new job when already active`() =
        runTest(testDispatcher) {
            val url = "https://github.com/test/repo"

            // Create a job that will block indefinitely
            coEvery { getRepository(url) } coAnswers {
                delay(8.seconds) // This will never complete
                Resource.Success(dummyRepository1)
            }

            viewModel.onUrlQueryChange(url)

            // Start first job
            viewModel.onAddLink()

            // Give the first job a chance to start
            testDispatcher.scheduler.advanceTimeBy(1)
            testDispatcher.scheduler.runCurrent()

            // Try to start second job immediately - should be ignored
            viewModel.onAddLink()

            // Verify that the use case was only called once
            coVerify(exactly = 1) { getRepository(url) }
        }

    @Test
    fun `onRemoveRepository should remove repository from preferences`() =
        runTest(testDispatcher) {
            val repositories = listOf(dummyRepository1, dummyRepository2)
            providerPreferencesFlow.value = ProviderPreferences(repositories = repositories)

            viewModel.onRemoveRepository(dummyRepository1)
            advanceUntilIdle()

            viewModel.repositories.test {
                val updatedRepositories = awaitItem()
                expectThat(updatedRepositories).hasSize(1)
                expectThat(updatedRepositories).contains(dummyRepository2)
            }
        }

    @Test
    fun `onRemoveRepository should not start new job when already active`() =
        runTest(testDispatcher) {
            val repositories = listOf(dummyRepository1, dummyRepository2)
            providerPreferencesFlow.value = ProviderPreferences(repositories = repositories)

            coEvery {
                dataStoreManager.updateUserPrefs<ProviderPreferences>(
                    UserPreferences.PROVIDER_PREFS_KEY,
                    ProviderPreferences::class,
                    any<suspend (ProviderPreferences) -> ProviderPreferences>(),
                )
            } coAnswers {
                delay(8.seconds) // This will never complete
                val transform = thirdArg<suspend (ProviderPreferences) -> ProviderPreferences>()
                val newPrefs = transform(providerPreferencesFlow.value)
                providerPreferencesFlow.value = newPrefs
            }

            viewModel.onRemoveRepository(dummyRepository1)

            // Give the first job a chance to start
            testDispatcher.scheduler.advanceTimeBy(1)
            testDispatcher.scheduler.runCurrent()

            viewModel.onRemoveRepository(dummyRepository2) // Should be ignored

            coVerify(exactly = 1) {
                dataStoreManager.updateUserPrefs<ProviderPreferences>(
                    UserPreferences.PROVIDER_PREFS_KEY,
                    ProviderPreferences::class,
                    any<suspend (ProviderPreferences) -> ProviderPreferences>(),
                )
            }
        }

    @Test
    fun `onRemoveSelection should remove all selected repositories`() =
        runTest(testDispatcher) {
            val repositories = listOf(dummyRepository1, dummyRepository2, dummyRepository3)
            providerPreferencesFlow.value = ProviderPreferences(repositories = repositories)

            viewModel.toggleRepositorySelection(dummyRepository1)
            viewModel.toggleRepositorySelection(dummyRepository2)
            viewModel.onRemoveSelection()
            advanceUntilIdle()

            viewModel.repositories.test {
                val updatedRepositories = awaitItem()
                expectThat(updatedRepositories).hasSize(1)
                expectThat(updatedRepositories).contains(dummyRepository3)
            }
        }

    @Test
    fun `onRemoveSelection should not start new job when already active`() =
        runTest(testDispatcher) {
            val repositories = listOf(dummyRepository1, dummyRepository2)
            providerPreferencesFlow.value = ProviderPreferences(repositories = repositories)

            coEvery {
                dataStoreManager.updateUserPrefs<ProviderPreferences>(
                    UserPreferences.PROVIDER_PREFS_KEY,
                    ProviderPreferences::class,
                    any<suspend (ProviderPreferences) -> ProviderPreferences>(),
                )
            } coAnswers {
                delay(8.seconds) // This will never complete
                val transform = thirdArg<suspend (ProviderPreferences) -> ProviderPreferences>()
                val newPrefs = transform(providerPreferencesFlow.value)
                providerPreferencesFlow.value = newPrefs
            }

            viewModel.toggleRepositorySelection(dummyRepository1)
            viewModel.onRemoveSelection()

            // Give the first job a chance to start
            testDispatcher.scheduler.advanceTimeBy(1)
            testDispatcher.scheduler.runCurrent()

            viewModel.onRemoveSelection() // Should be ignored

            coVerify(exactly = 1) {
                dataStoreManager.updateUserPrefs<ProviderPreferences>(
                    UserPreferences.PROVIDER_PREFS_KEY,
                    ProviderPreferences::class,
                    any<suspend (ProviderPreferences) -> ProviderPreferences>(),
                )
            }
        }

    @Test
    fun `onConsumeError should clear error from ui state`() =
        runTest(testDispatcher) {
            val errorMessage = UiText.StringResource(123)
            val url = "invalid-url"
            coEvery { getRepository(url) } returns Resource.Failure(errorMessage)

            viewModel.onUrlQueryChange(url)
            viewModel.onAddLink()
            advanceUntilIdle()

            viewModel.onConsumeError()

            viewModel.uiState.test {
                expectThat(awaitItem().error).isNull()
            }
        }

    @Test
    fun `multiple repository selections should work correctly`() =
        runTest(testDispatcher) {
            turbineScope {
                val selectedRepositoriesTurbine = viewModel.selectedRepositories.testIn(this)
                selectedRepositoriesTurbine.skipItems(1) // Skip initial empty emission

                viewModel.toggleRepositorySelection(dummyRepository1)
                val selection1 = selectedRepositoriesTurbine.awaitItem()
                expectThat(selection1).hasSize(1)
                expectThat(selection1).contains(dummyRepository1)

                viewModel.toggleRepositorySelection(dummyRepository2)
                val selection2 = selectedRepositoriesTurbine.awaitItem()
                expectThat(selection2).hasSize(2)
                expectThat(selection2).contains(dummyRepository1)
                expectThat(selection2).contains(dummyRepository2)

                viewModel.toggleRepositorySelection(dummyRepository3)
                val selection3 = selectedRepositoriesTurbine.awaitItem()
                expectThat(selection3).hasSize(3)
                expectThat(selection3).contains(dummyRepository1)
                expectThat(selection3).contains(dummyRepository2)
                expectThat(selection3).contains(dummyRepository3)

                // Toggle one off
                viewModel.toggleRepositorySelection(dummyRepository2)
                val selection4 = selectedRepositoriesTurbine.awaitItem()
                expectThat(selection4).hasSize(2)
                expectThat(selection4).contains(dummyRepository1)
                expectThat(selection4).contains(dummyRepository3)

                selectedRepositoriesTurbine.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `search should be case insensitive`() =
        runTest(testDispatcher) {
            val repositories = listOf(dummyRepository1, dummyRepository2, dummyRepository3)
            providerPreferencesFlow.value = ProviderPreferences(repositories = repositories)

            viewModel.onSearchQueryChange("REPO1")
            advanceUntilIdle()

            viewModel.searchResults.test {
                skipItems(1) // Skip initial empty emission
                val results = awaitItem()
                expectThat(results).hasSize(1)
                expectThat(results).contains(dummyRepository1)
            }
        }
}
