package com.flixclusive.feature.mobile.provider.add

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.ProviderInstallationStatus
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.feature.mobile.provider.add.filter.CommonSortFilters
import com.flixclusive.model.provider.ProviderManifest
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository
import com.flixclusive.provider.Provider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEmpty
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddProviderViewModelTest {
    private lateinit var viewModel: AddProviderViewModel
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var providerRepository: ProviderRepository
    private lateinit var getProviderFromRemote: GetProviderFromRemoteUseCase
    private lateinit var updateProvider: UpdateProviderUseCase
    private lateinit var loadProvider: LoadProviderUseCase
    private lateinit var unloadProvider: UnloadProviderUseCase
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var savedStateHandle: SavedStateHandle

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    private val testProvider1 = DummyDataForPreview.getDummyProviderMetadata(
        id = "test-provider-1",
        name = "Test Provider 1",
        versionName = "1.0.0",
        versionCode = 10000,
    )

    private val testProvider2 = DummyDataForPreview.getDummyProviderMetadata(
        id = "test-provider-2",
        name = "Test Provider 2",
        versionName = "2.0.0",
        versionCode = 20000,
    )

    private val testRepository = Repository(
        name = "test-repo",
        owner = "test-owner",
        url = "https://github.com/test-owner/test-repo",
        rawLinkFormat = "https://raw.githubusercontent.com/%s/%s/%s/providers.json",
    )

    private val testProviderPreferences = ProviderPreferences(
        repositories = listOf(testRepository),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(::infoLog)
        every { infoLog(any()) } answers {
            println(args[0])
            1
        }

        dataStoreManager = mockk(relaxed = true)
        providerRepository = mockk(relaxed = true)
        getProviderFromRemote = mockk(relaxed = true)
        updateProvider = mockk(relaxed = true)
        loadProvider = mockk(relaxed = true)
        unloadProvider = mockk(relaxed = true)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
        savedStateHandle = mockk(relaxed = true) {
            every { get<Repository?>("initialSelectedRepositoryFilter") } returns null
        }

        every {
            dataStoreManager.getUserPrefs(
                UserPreferences.PROVIDER_PREFS_KEY,
                ProviderPreferences::class,
            )
        } returns flowOf(testProviderPreferences)

        setupDefaultBehavior()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(::infoLog)
    }

    private fun setupDefaultBehavior() {
        coEvery { getProviderFromRemote(any()) } returns Resource.Success(listOf(testProvider1, testProvider2))
        every { providerRepository.getProviderMetadata(any()) } returns null
        every { providerRepository.getProvider(any()) } returns null
        coEvery { loadProvider(any()) } returns flow { emit(LoadProviderResult.Success(testProvider1)) }
        coEvery { unloadProvider(any()) } just runs
        coEvery { updateProvider(provider = any()) } just runs
    }

    private fun createViewModel() {
        viewModel = AddProviderViewModel(
            dataStoreManager = dataStoreManager,
            providerRepository = providerRepository,
            getProviderFromRemote = getProviderFromRemote,
            _updateProvider = updateProvider,
            loadProvider = loadProvider,
            unloadProvider = unloadProvider,
            appDispatchers = appDispatchers,
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `initialize loads providers and sets up filters`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.isLoading).isFalse()
                expectThat(state.repositoryExceptions).isEmpty()
            }

            viewModel.filters.test {
                val filters = awaitItem()
                expectThat(filters).hasSize(6)
            }

            expectThat(viewModel.providerInstallationStatusMap).hasSize(2)
            expectThat(viewModel.providerInstallationStatusMap[testProvider1.id])
                .isEqualTo(ProviderInstallationStatus.NotInstalled)
            expectThat(viewModel.providerInstallationStatusMap[testProvider2.id])
                .isEqualTo(ProviderInstallationStatus.NotInstalled)
        }

    @Test
    fun `initialize handles repository errors gracefully`() =
        runTest(testDispatcher) {
            val errorMessage = UiText.StringValue("Repository error")
            coEvery { getProviderFromRemote(testRepository) } returns Resource.Failure(errorMessage)

            createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.isLoading).isFalse()
                expectThat(state.repositoryExceptions).hasSize(1)
                expectThat(state.repositoryExceptions.first().first).isEqualTo(testRepository)
                expectThat(state.repositoryExceptions.first().second).isEqualTo(errorMessage)
            }
        }

    @Test
    fun `onSearchQueryChange updates search query`() =
        runTest(testDispatcher) {
            createViewModel()

            val testQuery = "test query"
            viewModel.onSearchQueryChange(testQuery)

            viewModel.searchQuery.test {
                expectThat(awaitItem()).isEqualTo(testQuery)
            }
        }

    @Test
    fun `onToggleSelect adds and removes providers from selection`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleSelect(testProvider1)

            viewModel.selected.test {
                expectThat(awaitItem()).isEqualTo(persistentSetOf(testProvider1))
            }

            viewModel.onToggleSelect(testProvider1)

            viewModel.selected.test {
                expectThat(awaitItem()).isEqualTo(persistentSetOf<ProviderMetadata>())
            }
        }

    @Test
    fun `onUnselectAll clears all selections`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleSelect(testProvider1)
            viewModel.onToggleSelect(testProvider2)
            viewModel.onUnselectAll()

            viewModel.selected.test {
                expectThat(awaitItem()).isEqualTo(persistentSetOf<ProviderMetadata>())
            }
        }

    @Test
    fun `onToggleSearchBar updates search bar visibility`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleSearchBar(true)

            viewModel.uiState.test {
                expectThat(awaitItem().isShowingSearchBar).isTrue()
            }

            viewModel.onToggleSearchBar(false)

            viewModel.uiState.test {
                expectThat(awaitItem().isShowingSearchBar).isFalse()
            }
        }

    @Test
    fun `onUpdateFilter updates filter at specified index`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            val newSortFilter = CommonSortFilters.create()
            viewModel.onUpdateFilter(0, newSortFilter)

            viewModel.filters.test {
                val filters = awaitItem()
                expectThat(filters[0]).isEqualTo(newSortFilter)
            }
        }

    @Test
    fun `onToggleInstallation installs not installed provider`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation(testProvider1)
            advanceUntilIdle()

            coVerify { loadProvider(testProvider1) }
            expectThat(viewModel.providerInstallationStatusMap[testProvider1.id])
                .isEqualTo(ProviderInstallationStatus.NotInstalled)
        }

    @Test
    fun `onToggleInstallation uninstalls installed provider`() =
        runTest(testDispatcher) {
            every { providerRepository.getProviderMetadata(testProvider1.id) } returns testProvider1

            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation(testProvider1)
            advanceUntilIdle()

            coVerify { unloadProvider(testProvider1) }
            expectThat(viewModel.providerInstallationStatusMap[testProvider1.id])
                .isEqualTo(ProviderInstallationStatus.NotInstalled)
        }

    @Test
    fun `onToggleInstallation updates outdated provider`() =
        runTest(testDispatcher) {
            val installedProvider = testProvider1.copy(versionCode = 5000)
            val mockProvider = mockk<Provider>(relaxed = true)
            val mockManifest = mockk<ProviderManifest>(relaxed = true)

            every { providerRepository.getProviderMetadata(testProvider1.id) } returns installedProvider
            every { providerRepository.getProvider(testProvider1.id) } returns mockProvider
            every { mockProvider.manifest } returns mockManifest
            every { mockManifest.updateUrl } returns "https://example.com/update"
            every { mockManifest.versionCode } returns 5000L

            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation(testProvider1)
            advanceUntilIdle()

            coVerify { updateProvider(testProvider1) }
            expectThat(viewModel.providerInstallationStatusMap[testProvider1.id])
                .isEqualTo(ProviderInstallationStatus.Installed)
        }

    @Test
    fun `onInstallSelection installs all selected providers`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleSelect(testProvider1)
            viewModel.onToggleSelect(testProvider2)

            viewModel.onInstallSelection()
            advanceUntilIdle()

            coVerify { loadProvider(testProvider1) }
            coVerify { loadProvider(testProvider2) }
        }

    @Test
    fun `consumeProviderExceptions clears provider exceptions`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.consumeProviderExceptions()

            viewModel.uiState.test {
                expectThat(awaitItem().providerExceptions).isEmpty()
            }
        }

    @Test
    fun `searchResults filters providers based on search query`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("Provider 1")
            advanceUntilIdle()

            viewModel.searchResults.test {
                skipItems(1) // Skip initial empty state
                val results = awaitItem()
                expectThat(results).hasSize(1)
                expectThat(results.first().metadata.name).isEqualTo("Test Provider 1")
            }
        }

    @Test
    fun `searchResults returns all providers when search query is blank`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("")
            advanceUntilIdle()

            viewModel.searchResults.test {
                skipItems(1) // Skip initial empty state
                val results = awaitItem()
                expectThat(results).hasSize(2)
            }
        }

    @Test
    fun `initialize prevents multiple concurrent initializations`() =
        runTest(testDispatcher) {
            createViewModel()

            // Call initialize multiple times quickly
            repeat(3) {
                viewModel.initialize()
            }

            advanceUntilIdle()

            // Verify getProviderFromRemote is only called once per repository
            coVerify(exactly = 1) { getProviderFromRemote(testRepository) }
        }

    @Test
    fun `installation handles provider errors gracefully`() =
        runTest(testDispatcher) {
            val exception = RuntimeException("Installation failed")
            coEvery { loadProvider(testProvider1) } returns flow { throw exception }

            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation(testProvider1)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.providerExceptions).isNotEmpty()
                expectThat(state.providerExceptions.first().provider).isEqualTo(testProvider1)
            }
        }

    @Test
    fun `update provider handles errors gracefully`() =
        runTest(testDispatcher) {
            val exception = RuntimeException("Update failed")
            val installedProvider = testProvider1.copy(versionCode = 5000)
            val mockProvider = mockk<Provider>(relaxed = true)
            val mockManifest = mockk<ProviderManifest>(relaxed = true)

            every { providerRepository.getProviderMetadata(testProvider1.id) } returns installedProvider
            every { providerRepository.getProvider(testProvider1.id) } returns mockProvider
            every { mockProvider.manifest } returns mockManifest
            every { mockManifest.updateUrl } returns "https://example.com/update"
            every { mockManifest.versionCode } returns 5000L
            coEvery { updateProvider(testProvider1) } throws exception

            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation(testProvider1)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.providerExceptions).isNotEmpty()
                expectThat(state.providerExceptions.first().provider).isEqualTo(testProvider1)
            }
        }

    @Test
    fun `uninstall provider handles errors gracefully`() =
        runTest(testDispatcher) {
            val exception = RuntimeException("Uninstall failed")
            every { providerRepository.getProviderMetadata(testProvider1.id) } returns testProvider1
            coEvery { unloadProvider(testProvider1) } throws exception

            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation(testProvider1)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.providerExceptions).isNotEmpty()
                expectThat(state.providerExceptions.first().provider).isEqualTo(testProvider1)
            }
        }
}
