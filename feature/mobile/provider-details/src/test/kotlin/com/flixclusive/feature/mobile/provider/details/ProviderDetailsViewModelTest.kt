package com.flixclusive.feature.mobile.provider.details

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
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.model.provider.ProviderManifest
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.Provider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class ProviderDetailsViewModelTest {
    private lateinit var viewModel: ProviderDetailsViewModel
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var loadProvider: LoadProviderUseCase
    private lateinit var unloadProvider: UnloadProviderUseCase
    private lateinit var updateProvider: UpdateProviderUseCase
    private lateinit var providerRepository: ProviderRepository
    private lateinit var getProviderFromRemote: GetProviderFromRemoteUseCase
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var savedStateHandle: SavedStateHandle

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    private val testProvider = DummyDataForPreview.getDummyProviderMetadata(
        id = "test-provider",
        name = "Test Provider",
        versionName = "1.0.0",
        versionCode = 10000,
        repositoryUrl = "https://github.com/test/test-repo",
    )

    private val testProviderPreferences = ProviderPreferences(
        shouldWarnBeforeInstall = true,
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
        loadProvider = mockk(relaxed = true)
        unloadProvider = mockk(relaxed = true)
        updateProvider = mockk(relaxed = true)
        providerRepository = mockk(relaxed = true)
        getProviderFromRemote = mockk(relaxed = true)
        appDispatchers = mockk(relaxed = true)
        savedStateHandle = mockk(relaxed = true)

        every { appDispatchers.ioScope } returns CoroutineScope(testDispatcher)

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

    @OptIn(ExperimentalSerializationApi::class)
    private fun setupDefaultBehavior() {
        every { savedStateHandle.get<ByteArray>("metadata") } answers {
            ByteArrayOutputStream().use {
                Json.encodeToStream(ProviderMetadata.serializer(), testProvider, it)
                it.toByteArray()
            }
        }
        every { providerRepository.getProviderMetadata(any()) } returns null
        every { providerRepository.getProvider(any()) } returns null
        coEvery { loadProvider(any()) } returns flow { emit(LoadProviderResult.Success(testProvider)) }
        coEvery { unloadProvider(any()) } just runs
        coEvery { updateProvider(provider = any()) } just runs
        coEvery { getProviderFromRemote(any(), any()) } returns Resource.Success(testProvider)
        coEvery {
            dataStoreManager.updateUserPrefs(
                key = any(),
                type = any(),
                transform = any<suspend (ProviderPreferences) -> ProviderPreferences>(),
            )
        } just runs
    }

    private fun createViewModel() {
        viewModel = ProviderDetailsViewModel(
            dataStoreManager = dataStoreManager,
            loadProvider = loadProvider,
            unloadProvider = unloadProvider,
            _updateProvider = updateProvider,
            providerRepository = providerRepository,
            getProviderFromRemote = getProviderFromRemote,
            appDispatchers = appDispatchers,
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `initial state should be correct for non-installed provider`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.metadata).isEqualTo(testProvider)
                expectThat(state.installationStatus).isEqualTo(ProviderInstallationStatus.NotInstalled)
                expectThat(state.installationError).isNull()
                expectThat(state.initializationError).isNull()
            }
        }

    @Test
    fun `initial state should be correct for installed provider`() =
        runTest(testDispatcher) {
            every { providerRepository.getProviderMetadata(testProvider.id) } returns testProvider

            createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.installationStatus).isEqualTo(ProviderInstallationStatus.Installed)
            }
        }

    @Test
    fun `initial state should be correct for outdated provider`() =
        runTest(testDispatcher) {
            val installedProvider = testProvider.copy(versionCode = 5000)
            val mockProvider = mockk<Provider>(relaxed = true)
            val mockManifest = mockk<ProviderManifest>(relaxed = true)
            val newerProvider = testProvider.copy(versionCode = 15000)

            every { providerRepository.getProviderMetadata(testProvider.id) } returns installedProvider
            every { providerRepository.getProvider(testProvider.id) } returns mockProvider
            every { mockProvider.manifest } returns mockManifest
            every { mockManifest.updateUrl } returns "https://example.com/update"
            every { mockManifest.versionCode } returns 5000L
            coEvery { getProviderFromRemote(any(), testProvider.id) } returns Resource.Success(newerProvider)

            createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.installationStatus).isEqualTo(ProviderInstallationStatus.Outdated)
            }
        }

    @Test
    fun `warnOnInstall flow emits correct initial value`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.warnOnInstall.test {
                expectThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `onToggleInstallation installs not installed provider`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation()
            advanceUntilIdle()

            coVerify { loadProvider(testProvider) }

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.installationStatus).isEqualTo(ProviderInstallationStatus.NotInstalled)
            }
        }

    @Test
    fun `onToggleInstallation uninstalls installed provider`() =
        runTest(testDispatcher) {
            every { providerRepository.getProviderMetadata(testProvider.id) } returns testProvider

            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation()
            advanceUntilIdle()

            coVerify { unloadProvider(testProvider) }

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.installationStatus).isEqualTo(ProviderInstallationStatus.NotInstalled)
            }
        }

    @Test
    fun `onToggleInstallation updates outdated provider`() =
        runTest(testDispatcher) {
            val installedProvider = testProvider.copy(versionCode = 5000)
            val mockProvider = mockk<Provider>(relaxed = true)
            val mockManifest = mockk<ProviderManifest>(relaxed = true)
            val newerProvider = testProvider.copy(versionCode = 15000)

            every { providerRepository.getProviderMetadata(testProvider.id) } returns installedProvider
            every { providerRepository.getProvider(testProvider.id) } returns mockProvider
            every { mockProvider.manifest } returns mockManifest
            every { mockManifest.updateUrl } returns "https://example.com/update"
            every { mockManifest.versionCode } returns 5000L
            coEvery { getProviderFromRemote(any(), testProvider.id) } returns Resource.Success(newerProvider)

            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation()
            advanceUntilIdle()

            coVerify { updateProvider(testProvider) }

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.installationStatus).isEqualTo(ProviderInstallationStatus.Installed)
            }
        }

    @Test
    fun `installation shows installing status during operation`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation()

            viewModel.uiState.test {
                skipItems(1) // Skip initial and NotInstalled states
                expectThat(awaitItem().installationStatus).isEqualTo(ProviderInstallationStatus.Installing)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `installation handles errors gracefully`() =
        runTest(testDispatcher) {
            val exception = RuntimeException("Installation failed")
            coEvery { loadProvider(testProvider) } returns flow { throw exception }

            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.installationError).isNotNull()
                expectThat(state.installationStatus).isEqualTo(ProviderInstallationStatus.NotInstalled)
            }
        }

    @Test
    fun `uninstallation handles errors gracefully`() =
        runTest(testDispatcher) {
            val exception = RuntimeException("Uninstall failed")
            every { providerRepository.getProviderMetadata(testProvider.id) } returns testProvider
            coEvery { unloadProvider(testProvider) } throws exception

            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.installationError).isNotNull()
            }
        }

    @Test
    fun `update provider handles errors gracefully`() =
        runTest(testDispatcher) {
            val exception = RuntimeException("Update failed")
            val installedProvider = testProvider.copy(versionCode = 5000)
            val mockProvider = mockk<Provider>(relaxed = true)
            val mockManifest = mockk<ProviderManifest>(relaxed = true)
            val newerProvider = testProvider.copy(versionCode = 15000)

            every { providerRepository.getProviderMetadata(testProvider.id) } returns installedProvider
            every { providerRepository.getProvider(testProvider.id) } returns mockProvider
            every { mockProvider.manifest } returns mockManifest
            every { mockManifest.updateUrl } returns "https://example.com/update"
            every { mockManifest.versionCode } returns 5000L
            coEvery { getProviderFromRemote(any(), testProvider.id) } returns Resource.Success(newerProvider)
            coEvery { updateProvider(testProvider) } throws exception

            createViewModel()
            advanceUntilIdle()

            viewModel.onToggleInstallation()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.installationError).isNotNull()
            }
        }

    @Test
    fun `disableWarnOnInstall updates preferences correctly`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.disableWarnOnInstall(false)
            advanceUntilIdle()

            coVerify {
                dataStoreManager.updateUserPrefs(
                    UserPreferences.PROVIDER_PREFS_KEY,
                    ProviderPreferences::class,
                    any<suspend (ProviderPreferences) -> ProviderPreferences>(),
                )
            }
        }

    @Test
    fun `initialization handles errors gracefully`() =
        runTest(testDispatcher) {
            val exception = RuntimeException("Initialization failed")
            every { providerRepository.getProviderMetadata(testProvider.id) } throws exception

            createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.initializationError).isNotNull()
            }
        }

    @Test
    fun `concurrent installations are prevented`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            // Start first installation
            viewModel.onToggleInstallation()

            // Try to start second installation immediately
            viewModel.onToggleInstallation()

            advanceUntilIdle()

            // Verify loadProvider is only called once
            coVerify(exactly = 1) { loadProvider(testProvider) }
        }

    @Test
    fun `isOutdated returns false when provider has no update url`() =
        runTest(testDispatcher) {
            val installedProvider = testProvider.copy(versionCode = 5000)
            val mockProvider = mockk<Provider>(relaxed = true)
            val mockManifest = mockk<ProviderManifest>(relaxed = true)

            every { providerRepository.getProviderMetadata(testProvider.id) } returns installedProvider
            every { providerRepository.getProvider(testProvider.id) } returns mockProvider
            every { mockProvider.manifest } returns mockManifest
            every { mockManifest.updateUrl } returns null

            createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.installationStatus).isEqualTo(ProviderInstallationStatus.Installed)
            }
        }

    @Test
    fun `isOutdated returns false when remote provider fetch fails`() =
        runTest(testDispatcher) {
            val installedProvider = testProvider.copy(versionCode = 5000)
            val mockProvider = mockk<Provider>(relaxed = true)
            val mockManifest = mockk<ProviderManifest>(relaxed = true)

            every { providerRepository.getProviderMetadata(testProvider.id) } returns installedProvider
            every { providerRepository.getProvider(testProvider.id) } returns mockProvider
            every { mockProvider.manifest } returns mockManifest
            every { mockManifest.updateUrl } returns "https://example.com/update"
            every { mockManifest.versionCode } returns 5000L
            coEvery {
                getProviderFromRemote(
                    any(),
                    testProvider.id,
                )
            } returns Resource.Failure(UiText.StringValue("Network error"))

            createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.installationStatus).isEqualTo(ProviderInstallationStatus.Installed)
            }
        }

    @Test
    fun `warnOnInstall flow updates when preferences change`() =
        runTest(testDispatcher) {
            val preferencesFlow = MutableStateFlow(testProviderPreferences)
            every {
                dataStoreManager.getUserPrefs(
                    UserPreferences.PROVIDER_PREFS_KEY,
                    ProviderPreferences::class,
                )
            } returns preferencesFlow

            createViewModel()
            advanceUntilIdle()

            viewModel.warnOnInstall.test {
                expectThat(awaitItem()).isTrue()

                preferencesFlow.value = testProviderPreferences.copy(shouldWarnBeforeInstall = false)
                expectThat(awaitItem()).isFalse()
            }
        }

    @Test
    fun `installation success updates status correctly when provider is found after installation`() =
        runTest(testDispatcher) {
            // Initially provider not installed
            every { providerRepository.getProviderMetadata(testProvider.id) } returns null

            createViewModel()
            advanceUntilIdle()

            // During installation completion, simulate provider being found
            every { providerRepository.getProviderMetadata(testProvider.id) } returns testProvider

            viewModel.onToggleInstallation()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.installationStatus).isEqualTo(ProviderInstallationStatus.Installed)
            }
        }
}
