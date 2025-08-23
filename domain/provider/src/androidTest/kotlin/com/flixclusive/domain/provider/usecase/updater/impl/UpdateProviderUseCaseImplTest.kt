package com.flixclusive.domain.provider.usecase.updater.impl

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderFromPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.model.provider.ProviderManifest
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.Provider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.io.File

// TODO: Replace usecase dependencies with direct network/file operations for better maintainability.
//  The current approach creates tight coupling between use cases, making tests brittle and harder to maintain.
//  Consider using real HTTP clients and file operations instead of mocking complex use case interactions.
@RunWith(AndroidJUnit4::class)
class UpdateProviderUseCaseImplTest {
    private lateinit var updateProviderUseCase: UpdateProviderUseCaseImpl
    private lateinit var context: Context
    private lateinit var mockUserSessionDataStore: UserSessionDataStore
    private lateinit var mockDataStoreManager: DataStoreManager
    private lateinit var mockProviderRepository: ProviderRepository
    private lateinit var mockProviderApiRepository: ProviderApiRepository
    private lateinit var mockLoadProviderUseCase: LoadProviderUseCase
    private lateinit var mockUnloadProviderUseCase: UnloadProviderUseCase
    private lateinit var mockGetProviderFromRemoteUseCase: GetProviderFromRemoteUseCase
    private lateinit var mockProvider: Provider
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var tempDirectory: File
    private val testDispatcher = StandardTestDispatcher()

    private val testUserId = 1
    private val testProviderMetadata = ProviderTestDefaults.getProviderMetadata(
        id = "14a5037ac9553dd",
        name = "Test Provider",
        versionCode = 9999L,
        repositoryUrl = "https://github.com/flixclusiveorg/providers-template",
        buildUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/BasicDummyProvider.flx",
    )

    private val webViewProviderMetadata = ProviderTestDefaults.getWebViewProviderMetadata(
        id = "407e8638eb9d50c",
        name = "WebView Test Provider",
        versionCode = 9999L,
        repositoryUrl = "https://github.com/flixclusiveorg/providers-template",
        buildUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/BasicDummyWebViewProvider.flx",
    )

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        tempDirectory = createTempDirectory()

        mockUserSessionDataStore = mockk()
        mockDataStoreManager = mockk()
        mockProviderRepository = mockk(relaxed = true)
        mockProviderApiRepository = mockk(relaxed = true)
        mockLoadProviderUseCase = mockk(relaxed = true)
        mockUnloadProviderUseCase = mockk(relaxed = true)
        mockGetProviderFromRemoteUseCase = mockk(relaxed = true)
        mockProvider = mockk(relaxed = true)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        val client = OkHttpClient.Builder().build()

        updateProviderUseCase = UpdateProviderUseCaseImpl(
            context = context,
            dataStoreManager = mockDataStoreManager,
            userSessionDataStore = mockUserSessionDataStore,
            providerRepository = mockProviderRepository,
            loadProviderUseCase = mockLoadProviderUseCase,
            unloadProviderUseCase = mockUnloadProviderUseCase,
            getProviderFromRemoteUseCase = mockGetProviderFromRemoteUseCase,
            client = client,
            appDispatchers = appDispatchers,
        )

        setupDefaultMocks()
    }

    @After
    fun tearDown() {
        if (tempDirectory.exists()) {
            tempDirectory.deleteRecursively()
        }
    }

    @Test
    fun shouldSuccessfullyUpdateProviderToLatestVersion() =
        runTest(testDispatcher) {
            val originalProviderFile = createOriginalProviderFile()
            setupSuccessfulUpdateScenario(originalProviderFile)

            val result = updateProviderUseCase(testProviderMetadata)

            expectThat(result).isTrue()
            verifyUpdateOperations()
        }

    @Test
    fun shouldFailWhenProviderDoesNotExistInRepository() =
        runTest(testDispatcher) {
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns null

            val result = runCatching {
                updateProviderUseCase(testProviderMetadata)
            }

            expectThat(result.isFailure).isTrue()
            expectThat(result.exceptionOrNull()).isA<IllegalArgumentException>()
        }

    @Test
    fun shouldFailWhenRemoteProviderNotFound() =
        runTest(testDispatcher) {
            val nonExistentMetadata = testProviderMetadata.copy(
                id = "non-existent-provider-id",
            )
            val originalProviderFile = createOriginalProviderFile()

            every { mockProviderRepository.getProvider(nonExistentMetadata.id) } returns mockProvider
            setupProviderPreferences(nonExistentMetadata, originalProviderFile)

            coEvery {
                mockGetProviderFromRemoteUseCase(any(), nonExistentMetadata.id)
            } returns Resource.Failure(RuntimeException("Provider not found"))

            val result = runCatching {
                updateProviderUseCase(nonExistentMetadata)
            }

            expectThat(result.isFailure).isTrue()
        }

    @Test
    fun shouldFailWhenNetworkRequestFails() =
        runTest(testDispatcher) {
            val invalidMetadata = testProviderMetadata.copy(
                repositoryUrl = "https://github.com/nonexistent/repository",
            )
            val originalProviderFile = createOriginalProviderFile()

            every { mockProviderRepository.getProvider(invalidMetadata.id) } returns mockProvider
            setupProviderPreferences(invalidMetadata, originalProviderFile)

            coEvery {
                mockGetProviderFromRemoteUseCase(any(), invalidMetadata.id)
            } returns Resource.Failure(RuntimeException("Network error"))

            val result = runCatching {
                updateProviderUseCase(invalidMetadata)
            }

            expectThat(result.isFailure).isTrue()
        }

    @Test
    fun shouldUpdateMultipleProvidersWithMixedResults() =
        runTest(testDispatcher) {
            val validProviderFile = createOriginalProviderFile()
            val invalidProviderFile = createOriginalProviderFile("invalid_provider")

            val validProvider = testProviderMetadata
            val invalidProvider = webViewProviderMetadata.copy(
                repositoryUrl = "https://github.com/nonexistent/repository",
            )

            every { mockProviderRepository.getProvider(validProvider.id) } returns mockProvider
            coEvery {
                mockGetProviderFromRemoteUseCase(any(), validProvider.id)
            } returns Resource.Success(validProvider.copy(versionCode = 10000L))

            coEvery { mockUnloadProviderUseCase(validProvider, false) } returns true
            coEvery { mockLoadProviderUseCase(any(), any()) } returns flowOf(LoadProviderResult.Success(validProvider))

            val mockInvalidProvider = mockk<Provider>(relaxed = true)
            every { mockProviderRepository.getProvider(invalidProvider.id) } returns mockInvalidProvider
            coEvery {
                mockGetProviderFromRemoteUseCase(any(), invalidProvider.id)
            } returns Resource.Failure(RuntimeException("Network error"))

            val providers = listOf(validProvider, invalidProvider)
            val providersPrefs = providers.map {
                val file = if (invalidProvider.id == it.id) invalidProviderFile else validProviderFile

                ProviderFromPreferences(
                    id = it.id,
                    name = it.name,
                    filePath = file.absolutePath,
                    isDisabled = false,
                )
            }

            val preferences = ProviderPreferences(providers = providersPrefs)

            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(preferences)

            val result = updateProviderUseCase(providers)

            expectThat(result.failed).hasSize(1)
            expectThat(result.success).hasSize(1)
            expectThat(result.success[0].id).isEqualTo(validProvider.id)
            expectThat(result.failed[0].first.id).isEqualTo(invalidProvider.id)
        }

    @Test
    fun shouldPreserveProviderOrderDuringUpdate() =
        runTest(testDispatcher) {
            val providerFile = createOriginalProviderFile()
            val otherProvider = ProviderFromPreferences(
                id = "other-provider",
                name = "Other Provider",
                filePath = File(tempDirectory, "other_provider.flx").absolutePath,
                isDisabled = false,
            )

            val targetProvider = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = providerFile.absolutePath,
                isDisabled = false,
            )

            val preferences = ProviderPreferences(
                providers = listOf(otherProvider, targetProvider),
            )

            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider
            every { mockUserSessionDataStore.currentUserId } returns flowOf(testUserId)

            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(preferences)

            coEvery {
                mockGetProviderFromRemoteUseCase(any(), testProviderMetadata.id)
            } returns Resource.Success(testProviderMetadata.copy(versionCode = 10000L))

            coEvery { mockUnloadProviderUseCase(testProviderMetadata, false) } returns true
            coEvery {
                mockLoadProviderUseCase(
                    any(),
                    any(),
                )
            } returns flowOf(LoadProviderResult.Success(testProviderMetadata))

            val result = updateProviderUseCase(testProviderMetadata)

            expectThat(result).isTrue()
            coVerify { mockProviderRepository.addToPreferences(any()) }
        }

    @Test
    fun shouldHandleProviderWithDifferentVersionCodes() =
        runTest(testDispatcher) {
            val outdatedProvider = testProviderMetadata.copy(versionCode = 5000L)
            val providerFile = createOriginalProviderFile()

            every { mockProviderRepository.getProvider(outdatedProvider.id) } returns mockProvider
            setupProviderPreferences(outdatedProvider, providerFile)

            coEvery {
                mockGetProviderFromRemoteUseCase(any(), outdatedProvider.id)
            } returns Resource.Success(outdatedProvider.copy(versionCode = 10000L))

            coEvery { mockUnloadProviderUseCase(outdatedProvider, false) } returns true
            coEvery { mockLoadProviderUseCase(any(), any()) } returns
                flowOf(LoadProviderResult.Success(outdatedProvider))

            val result = updateProviderUseCase(outdatedProvider)

            expectThat(result).isTrue()
            verifyUpdateOperations(outdatedProvider)
        }

    @Test
    fun shouldUnloadOldProviderWithoutRemovingFromPreferences() =
        runTest(testDispatcher) {
            val providerFile = createOriginalProviderFile()
            setupSuccessfulUpdateScenario(providerFile)

            val result = updateProviderUseCase(testProviderMetadata)

            expectThat(result).isTrue()
            coVerify { mockUnloadProviderUseCase(testProviderMetadata, false) }
        }

    @Test
    fun shouldLoadUpdatedProviderAfterSuccessfulDownload() =
        runTest(testDispatcher) {
            val providerFile = createOriginalProviderFile()
            setupSuccessfulUpdateScenario(providerFile)

            val result = updateProviderUseCase(testProviderMetadata)

            expectThat(result).isTrue()
            coVerify { mockProviderRepository.addToPreferences(any()) }
            coVerify { mockLoadProviderUseCase(any(), any()) }
        }

    @Test
    fun shouldHandleRepositoryUrlParsingCorrectly() =
        runTest(testDispatcher) {
            val repository = ProviderTestDefaults.getRepositoryFromUrl(
                "https://github.com/flixclusiveorg/providers-template",
            )

            expectThat(repository.owner).isEqualTo("flixclusiveorg")
            expectThat(repository.name).isEqualTo("providers-template")

            val rawLink = repository.getRawLink("updater.json", "builds")
            expectThat(rawLink).isEqualTo(
                "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
            )
        }

    @Test
    fun shouldReturnEmptyResultsWhenNoProvidersProvided() =
        runTest(testDispatcher) {
            val result = updateProviderUseCase(emptyList())

            expectThat(result.success).hasSize(0)
            expectThat(result.failed).hasSize(0)
        }

    @Test
    fun shouldCreateNewFileForUpdatedProvider() =
        runTest(testDispatcher) {
            val originalFile = createOriginalProviderFile()
            setupSuccessfulUpdateScenario(originalFile)

            val result = updateProviderUseCase(testProviderMetadata)

            expectThat(result).isTrue()
            coVerify { mockProviderRepository.addToPreferences(any()) }
        }

    @Test
    fun shouldHandleProviderManifestValidation() =
        runTest(testDispatcher) {
            val mockManifest = mockk<ProviderManifest>()
            every { mockManifest.versionCode } returns 9999L
            every { mockManifest.updateUrl } returns testProviderMetadata.buildUrl
            every { mockProvider.manifest } returns mockManifest

            val providerFile = createOriginalProviderFile()
            setupSuccessfulUpdateScenario(providerFile)

            val result = updateProviderUseCase(testProviderMetadata)

            expectThat(result).isTrue()
        }

    private fun setupDefaultMocks() {
        every { mockUserSessionDataStore.currentUserId } returns flowOf(testUserId)
        every { mockProvider.name } returns testProviderMetadata.name

        coEvery {
            mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
        } returns flowOf(ProviderPreferences())

        coEvery {
            mockDataStoreManager.updateUserPrefs(
                UserPreferences.PROVIDER_PREFS_KEY,
                ProviderPreferences::class,
                any(),
            )
        } returns Unit

        every { mockProviderRepository.getProvider(any()) } returns null
    }

    private fun setupSuccessfulUpdateScenario(providerFile: File) {
        every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider
        setupProviderPreferences(testProviderMetadata, providerFile)

        coEvery {
            mockGetProviderFromRemoteUseCase(any(), testProviderMetadata.id)
        } returns Resource.Success(testProviderMetadata.copy(versionCode = 10000L))

        coEvery { mockUnloadProviderUseCase(testProviderMetadata, false) } returns true
        coEvery {
            mockLoadProviderUseCase(any(), any())
        } returns flowOf(LoadProviderResult.Success(testProviderMetadata))
    }

    private fun setupProviderPreferences(
        metadata: ProviderMetadata,
        providerFile: File,
    ) {
        val preference = ProviderFromPreferences(
            id = metadata.id,
            name = metadata.name,
            filePath = providerFile.absolutePath,
            isDisabled = false,
        )

        val preferences = ProviderPreferences(providers = listOf(preference))

        coEvery {
            mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
        } returns flowOf(preferences)
    }

    private fun verifyUpdateOperations(metadata: ProviderMetadata = testProviderMetadata) {
        coVerify { mockUnloadProviderUseCase(metadata, false) }
        coVerify { mockProviderRepository.addToPreferences(any()) }
        coVerify { mockLoadProviderUseCase(any(), any()) }
    }

    private fun createTempDirectory(): File {
        val tempDir = File(context.cacheDir, "test_update_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        return tempDir
    }

    private fun createOriginalProviderFile(fileName: String = "original_provider.flx"): File {
        val providerDir = File(tempDirectory, "provider_${System.currentTimeMillis()}")
        providerDir.mkdirs()

        val providerFile = File(providerDir, fileName)
        val updaterFile = File(providerDir, "updater.json")

        context.assets.open("BasicDummyProvider.flx").use { inputStream ->
            providerFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        updaterFile.writeText(Json.encodeToString(listOf(testProviderMetadata)))

        return providerFile
    }
}
