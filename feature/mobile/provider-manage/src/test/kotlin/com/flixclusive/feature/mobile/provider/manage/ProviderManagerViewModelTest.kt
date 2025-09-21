package com.flixclusive.feature.mobile.provider.manage

import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderFromPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserOnBoarding
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.util.collections.CollectionsOperation
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.doesNotContain
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProviderManagerViewModelTest {
    private lateinit var viewModel: ProviderManagerViewModel
    private lateinit var unloadProvider: UnloadProviderUseCase
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var providerRepository: ProviderRepository
    private lateinit var providerApiRepository: ProviderApiRepository
    private lateinit var appDispatchers: AppDispatchers

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())
    private val providerRepositoryObserveFlow = MutableSharedFlow<CollectionsOperation.List<ProviderFromPreferences>>()

    private val testProvider1 = DummyDataForPreview.getDummyProviderMetadata(
        id = "test-provider-1",
        name = "Test Provider 1",
        versionName = "1.0.0",
    )

    private val testProvider2 = DummyDataForPreview.getDummyProviderMetadata(
        id = "test-provider-2",
        name = "Test Provider 2",
        versionName = "2.0.0",
    )

    private val testProviderFromPrefs1 = ProviderFromPreferences(
        id = testProvider1.id,
        name = testProvider1.name,
        filePath = "",
        isDisabled = false,
    )

    private val testProviderFromPrefs2 = ProviderFromPreferences(
        id = testProvider2.id,
        name = testProvider2.name,
        filePath = "",
        isDisabled = true,
    )

    private val testProviderPreferences = ProviderPreferences(
        providers = listOf(testProviderFromPrefs1, testProviderFromPrefs2),
    )

    private val testUserOnBoarding = UserOnBoarding(
        isFirstTimeOnProvidersScreen = true,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        unloadProvider = mockk(relaxed = true)
        dataStoreManager = mockk(relaxed = true)
        providerRepository = mockk(relaxed = true)
        providerApiRepository = mockk(relaxed = true)
        appDispatchers = mockk(relaxed = true)

        every { appDispatchers.ioScope } returns CoroutineScope(testDispatcher)

        every {
            dataStoreManager.getUserPrefs(
                UserPreferences.PROVIDER_PREFS_KEY,
                ProviderPreferences::class,
            )
        } returns flowOf(testProviderPreferences)

        every {
            dataStoreManager.getUserPrefs(
                UserPreferences.USER_ON_BOARDING_PREFS_KEY,
                UserOnBoarding::class,
            )
        } returns flowOf(testUserOnBoarding)

        setupDefaultBehavior()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupDefaultBehavior() {
        every { providerRepository.getOrderedProviders() } returns listOf(testProvider1, testProvider2)
        every { providerRepository.observe() } returns providerRepositoryObserveFlow
        every { providerRepository.getProviderMetadata(testProvider1.id) } returns testProvider1
        every { providerRepository.getProviderMetadata(testProvider2.id) } returns testProvider2
        every { providerRepository.getProviderFromPreferences(any()) } returns testProviderFromPrefs1
        coEvery { providerRepository.moveProvider(any(), any()) } just runs
        coEvery { providerRepository.toggleProvider(any()) } just runs
        coEvery { unloadProvider(any()) } just runs
        coEvery { providerApiRepository.removeApi(any()) } just runs
        coEvery { providerApiRepository.addApiFromId(any()) } just runs
        coEvery {
            dataStoreManager.updateUserPrefs(
                any(),
                any(),
                any<suspend (UserOnBoarding) -> UserOnBoarding>(),
            )
        } just runs
    }

    private fun createViewModel() {
        viewModel = ProviderManagerViewModel(
            unloadProvider = unloadProvider,
            dataStoreManager = dataStoreManager,
            providerRepository = providerRepository,
            providerApiRepository = providerApiRepository,
            appDispatchers = appDispatchers,
        )
    }

    @Test
    fun `initial state should be correct`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.isSearching).isFalse()
                expectThat(state.error).isNull()
            }

            expectThat(viewModel.providers).hasSize(2)
            expectThat(viewModel.providers[0]).isEqualTo(testProvider1)
            expectThat(viewModel.providers[1]).isEqualTo(testProvider2)
        }

    @Test
    fun `isFirstTimeOnProvidersScreen flow emits correct initial value`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.isFirstTimeOnProvidersScreen.test {
                expectThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `providerToggles flow emits correct values`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.providerToggles.test {
                val toggles = awaitItem()
                expectThat(toggles).hasSize(2)
                expectThat(toggles[0]).isFalse() // testProviderFromPrefs1.isDisabled
                expectThat(toggles[1]).isTrue() // testProviderFromPrefs2.isDisabled
            }
        }

    @Test
    fun `onQueryChange updates search query with debounce`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onQueryChange("test")
            advanceTimeBy(500) // Less than debounce time

            viewModel.searchQuery.test {
                expectThat(awaitItem()).isEqualTo("") // Should still be initial value

                advanceTimeBy(400) // Complete debounce time (800ms total)

                expectThat(awaitItem()).isEqualTo("test")
            }
        }

    @Test
    fun `onMove calls repository moveProvider`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onMove(0, 1)
            advanceUntilIdle()

            coVerify { providerRepository.moveProvider(0, 1) }
        }

    @Test
    fun `toggleProvider enables disabled provider successfully`() =
        runTest(testDispatcher) {
            every { providerRepository.getProviderFromPreferences(testProvider1.id) } returns
                testProviderFromPrefs1.copy(isDisabled = false)

            createViewModel()
            advanceUntilIdle()

            viewModel.toggleProvider(testProvider1.id)
            advanceUntilIdle()

            coVerify { providerRepository.toggleProvider(testProvider1.id) }
            coVerify { providerApiRepository.addApiFromId(testProvider1.id) }
        }

    @Test
    fun `toggleProvider disables enabled provider successfully`() =
        runTest(testDispatcher) {
            every { providerRepository.getProviderFromPreferences(testProvider1.id) } returns
                testProviderFromPrefs1.copy(isDisabled = true)

            createViewModel()
            advanceUntilIdle()

            viewModel.toggleProvider(testProvider1.id)
            advanceUntilIdle()

            coVerify { providerRepository.toggleProvider(testProvider1.id) }
            coVerify { providerApiRepository.removeApi(testProvider1.id) }
        }

    @Test
    fun `toggleProvider handles addApiFromId error gracefully`() =
        runTest(testDispatcher) {
            val exception = RuntimeException("API initialization failed")
            every { providerRepository.getProviderFromPreferences(testProvider1.id) } returns
                testProviderFromPrefs1.copy(isDisabled = false)
            coEvery { providerApiRepository.addApiFromId(testProvider1.id) } throws exception

            createViewModel()
            advanceUntilIdle()

            viewModel.toggleProvider(testProvider1.id)
            advanceUntilIdle()

            // Should rollback the toggle
            coVerify(exactly = 2) { providerRepository.toggleProvider(testProvider1.id) }

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.error).isNotNull()
                expectThat(state.error!!.provider).isEqualTo(testProvider1)
                expectThat(state.error.throwable).isEqualTo(exception)
            }
        }

    @Test
    fun `uninstallProvider calls unloadProvider use case`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.uninstallProvider(testProvider1)
            advanceUntilIdle()

            coVerify { unloadProvider(testProvider1) }
        }

    @Test
    fun `setFirstTimeOnProvidersScreen updates preferences`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.setFirstTimeOnProvidersScreen(false)
            advanceUntilIdle()

            coVerify {
                dataStoreManager.updateUserPrefs(
                    UserPreferences.USER_ON_BOARDING_PREFS_KEY,
                    UserOnBoarding::class,
                    any<suspend (UserOnBoarding) -> UserOnBoarding>(),
                )
            }
        }

    @Test
    fun `onConsumeError clears error state`() =
        runTest(testDispatcher) {
            // First set an error
            val exception = RuntimeException("Test error")
            every { providerRepository.getProviderFromPreferences(testProvider1.id) } returns
                testProviderFromPrefs1.copy(isDisabled = false)
            coEvery { providerApiRepository.addApiFromId(testProvider1.id) } throws exception

            createViewModel()
            advanceUntilIdle()

            viewModel.toggleProvider(testProvider1.id)
            advanceUntilIdle()

            // Verify error is set
            viewModel.uiState.test {
                expectThat(awaitItem().error).isNotNull()

                // Clear error
                viewModel.onConsumeError()

                expectThat(awaitItem().error).isNull()
            }
        }

    @Test
    fun `onToggleSearchBar updates search state`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onToggleSearchBar(true)

            viewModel.uiState.test {
                expectThat(awaitItem().isSearching).isTrue()
            }

            viewModel.onToggleSearchBar(false)

            viewModel.uiState.test {
                expectThat(awaitItem().isSearching).isFalse()
            }
        }

    @Test
    fun `providersChangesHandler handles add operation`() =
        runTest(testDispatcher) {
            val newProvider = DummyDataForPreview.getDummyProviderMetadata(
                id = "new-provider",
                name = "New Provider",
            )
            val newProviderFromPrefs = ProviderFromPreferences(
                id = newProvider.id,
                isDisabled = false,
                name = newProvider.name,
                filePath = "",
            )

            every { providerRepository.getProviderMetadata(newProvider.id) } returns newProvider

            createViewModel()
            advanceUntilIdle()

            // Initial providers count
            expectThat(viewModel.providers).hasSize(2)

            // Simulate add operation
            providerRepositoryObserveFlow.emit(CollectionsOperation.List.Add(newProviderFromPrefs))
            advanceUntilIdle()

            expectThat(viewModel.providers).hasSize(3)
            expectThat(viewModel.providers).contains(newProvider)
        }

    @Test
    fun `providersChangesHandler handles remove operation`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            // Initial providers count
            expectThat(viewModel.providers).hasSize(2)

            // Simulate remove operation
            providerRepositoryObserveFlow.emit(CollectionsOperation.List.Remove(testProviderFromPrefs1))
            advanceUntilIdle()

            expectThat(viewModel.providers).hasSize(1)
            expectThat(viewModel.providers).doesNotContain(testProvider1)
        }

    @Test
    fun `concurrent toggle operations are prevented`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            // Start first toggle operation
            viewModel.toggleProvider(testProvider1.id)

            // Try to start second toggle operation immediately (should be ignored)
            viewModel.toggleProvider(testProvider2.id)

            advanceUntilIdle()

            // Verify only the first toggle was processed
            coVerify(exactly = 1) { providerRepository.toggleProvider(testProvider1.id) }
            coVerify(exactly = 0) { providerRepository.toggleProvider(testProvider2.id) }
        }

    @Test
    fun `concurrent uninstall operations are prevented`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            // Start first uninstall operation
            viewModel.uninstallProvider(testProvider1)

            // Try to start second uninstall operation immediately (should be ignored)
            viewModel.uninstallProvider(testProvider2)

            advanceUntilIdle()

            // Verify only the first uninstall was processed
            coVerify(exactly = 1) { unloadProvider(testProvider1) }
            coVerify(exactly = 0) { unloadProvider(testProvider2) }
        }

    @Test
    fun `search query debounce prevents excessive updates`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            // Rapid query changes
            viewModel.onQueryChange("a")
            advanceTimeBy(100)
            viewModel.onQueryChange("ab")
            advanceTimeBy(100)
            viewModel.onQueryChange("abc")
            advanceTimeBy(100)
            viewModel.onQueryChange("abcd")

            // Don't advance full debounce time yet
            advanceTimeBy(500)

            viewModel.searchQuery.test {
                // Should still be initial empty value due to debounce
                expectThat(awaitItem()).isEqualTo("")

                // Complete debounce time
                advanceTimeBy(200)

                // Should now have the last query value
                expectThat(awaitItem()).isEqualTo("abcd")
            }
        }

    @Test
    fun `onboarding flow updates when datastore changes`() =
        runTest(testDispatcher) {
            val onboardingFlow = MutableStateFlow(testUserOnBoarding)
            every {
                dataStoreManager.getUserPrefs(
                    UserPreferences.USER_ON_BOARDING_PREFS_KEY,
                    UserOnBoarding::class,
                )
            } returns onboardingFlow

            createViewModel()
            advanceUntilIdle()

            viewModel.isFirstTimeOnProvidersScreen.test {
                expectThat(awaitItem()).isTrue()

                onboardingFlow.value = testUserOnBoarding.copy(isFirstTimeOnProvidersScreen = false)
                expectThat(awaitItem()).isFalse()
            }
        }
}
