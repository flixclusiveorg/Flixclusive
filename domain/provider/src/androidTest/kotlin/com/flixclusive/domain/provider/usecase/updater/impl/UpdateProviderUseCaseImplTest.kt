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
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.util.extensions.DownloadFailed
import com.flixclusive.model.provider.ProviderManifest
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.Provider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isNotEmpty
import strikt.assertions.isTrue
import java.io.File

// TODO: Replace usecase dependencies with direct network/file operations for better maintainability.
// The current approach creates tight coupling between use cases, making tests brittle and harder to maintain.
//  Consider using real HTTP clients and file operations instead of mocking complex use case interactions.
@RunWith(AndroidJUnit4::class)
class UpdateProviderUseCaseImplTest {
    private lateinit var updateProviderUseCase: UpdateProviderUseCaseImpl
    private lateinit var context: Context
    private lateinit var mockUserSessionDataStore: UserSessionDataStore
    private lateinit var mockDataStoreManager: DataStoreManager
    private lateinit var mockProviderRepository: ProviderRepository
    private lateinit var mockLoadProviderUseCase: LoadProviderUseCase
    private lateinit var mockUnloadProviderUseCase: UnloadProviderUseCase
    private lateinit var mockGetProviderFromRemoteUseCase: GetProviderFromRemoteUseCase
    private lateinit var mockProvider: Provider
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var tempDirectory: File
    private val testDispatcher = StandardTestDispatcher()

    private val buildUrlPrefix = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds"

    private val testUserId = 1
    private val testProviderMetadata = ProviderTestDefaults.getProviderMetadata(
        id = "14a5037ac9553dd",
        name = "Test Provider",
        versionCode = 9999L,
        repositoryUrl = "https://github.com/flixclusiveorg/providers-template",
        buildUrl = "$buildUrlPrefix/BasicDummyProvider.flx",
    )

    private val webViewProviderMetadata = ProviderTestDefaults.getWebViewProviderMetadata(
        id = "407e8638eb9d50c",
        name = "WebView Test Provider",
        versionCode = 9999L,
        repositoryUrl = "https://github.com/flixclusiveorg/providers-template",
        buildUrl = "$buildUrlPrefix/BasicDummyWebViewProvider.flx",
    )

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        tempDirectory = createTempDirectory()

        mockUserSessionDataStore = mockk()
        mockDataStoreManager = mockk()
        mockProviderRepository = mockk(relaxed = true)
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

            val result = runCatching { updateProviderUseCase(testProviderMetadata) }
                .onFailure {
                    throw it
                }

            expectThat(result.isSuccess).isTrue()
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
            expectThat(result.exceptionOrNull()).isA<IllegalStateException>()
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
    fun shouldThrowDownloadExceptionWhenNetworkRequestFails() =
        runTest(testDispatcher) {
            val invalidMetadata = testProviderMetadata.copy(
                buildUrl = "https://invalid-url.com/provider.flx",
            )
            val originalProviderFile = createOriginalProviderFile()

            every { mockProviderRepository.getProvider(invalidMetadata.id) } returns mockProvider
            setupProviderPreferences(invalidMetadata, originalProviderFile)

            coEvery {
                mockGetProviderFromRemoteUseCase(any(), invalidMetadata.id)
            } returns Resource.Success(invalidMetadata.copy(versionCode = 10000L))

            val result = runCatching {
                updateProviderUseCase(invalidMetadata)
            }

            expectThat(result.isFailure).isTrue()
            expectThat(result.exceptionOrNull()?.cause).isA<Throwable>()
        }

    @Test
    fun shouldRestoreBackupWhenLoadFailsAndProviderWasNotPreviouslyLoaded() =
        runTest(testDispatcher) {
            val providerFile = createOriginalProviderFile()

            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider andThen null
            setupProviderPreferences(testProviderMetadata, providerFile)

            coEvery {
                mockGetProviderFromRemoteUseCase(any(), testProviderMetadata.id)
            } returns Resource.Success(testProviderMetadata.copy(versionCode = 10000L))

            coEvery { mockUnloadProviderUseCase(testProviderMetadata, false) } just runs

            // Mock the load to fail initially, then succeed on backup restore
            coEvery {
                mockLoadProviderUseCase(any(), any())
            } returns flowOf(
                LoadProviderResult.Failure(
                    provider = testProviderMetadata,
                    filePath = providerFile.absolutePath,
                    error = RuntimeException("Load failed"),
                ),
            ) andThen flowOf(LoadProviderResult.Success(testProviderMetadata))

            val result = runCatching {
                updateProviderUseCase(testProviderMetadata)
            }

            expectThat(result.isFailure).isTrue()
            expectThat(result.exceptionOrNull()?.cause).isA<Throwable>()

            // Verify backup restoration happened
            coVerify(exactly = 2) { mockProviderRepository.addToPreferences(any()) }
            coVerify(exactly = 2) { mockLoadProviderUseCase(any(), any()) }
        }

    @Test
    fun shouldLogErrorButContinueWhenLoadFailsAndProviderWasPreviouslyLoaded() =
        runTest(testDispatcher) {
            val providerFile = createOriginalProviderFile()

            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider
            setupProviderPreferences(testProviderMetadata, providerFile)

            coEvery {
                mockGetProviderFromRemoteUseCase(any(), testProviderMetadata.id)
            } returns Resource.Success(testProviderMetadata.copy(versionCode = 10000L))

            coEvery { mockUnloadProviderUseCase(testProviderMetadata, false) } just runs

            // Mock the load to fail
            coEvery {
                mockLoadProviderUseCase(any(), any())
            } returns flowOf(
                LoadProviderResult.Failure(
                    provider = testProviderMetadata,
                    filePath = providerFile.absolutePath,
                    error = RuntimeException("Load failed"),
                ),
            )

            // Provider is already loaded, so should just log and continue
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = runCatching {
                updateProviderUseCase(testProviderMetadata)
            }

            expectThat(result.isSuccess).isTrue()

            // Verify no backup restoration (only one addToPreferences call)
            coVerify(exactly = 1) { mockProviderRepository.addToPreferences(any()) }
            coVerify(exactly = 1) { mockLoadProviderUseCase(any(), any()) }
        }

    @Test
    fun shouldCreateNewFilePathForUpdatedProviderWithDifferentName() =
        runTest(testDispatcher) {
            val providerFile = createOriginalProviderFile()
            val updatedMetadata = testProviderMetadata.copy(
                name = "Updated Provider Name",
                versionCode = 10000L,
            )

            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider
            setupProviderPreferences(testProviderMetadata, providerFile)

            coEvery {
                mockGetProviderFromRemoteUseCase(any(), testProviderMetadata.id)
            } returns Resource.Success(updatedMetadata)

            coEvery { mockUnloadProviderUseCase(testProviderMetadata, false) } just runs
            coEvery {
                mockLoadProviderUseCase(any(), any())
            } returns flowOf(LoadProviderResult.Success(updatedMetadata))

            val result = runCatching {
                updateProviderUseCase(testProviderMetadata)
            }

            expectThat(result.isSuccess).isTrue()

            // Verify that a new preference item was added with updated name
            coVerify {
                mockProviderRepository.addToPreferences(
                    match<ProviderFromPreferences> {
                        it.name == "Updated Provider Name" && it.id == testProviderMetadata.id
                    },
                )
            }
        }

    @Test
    fun shouldHandleMultipleProvidersWithDownloadFailuresCorrectly() =
        runTest(testDispatcher) {
            val validProviderFile = createOriginalProviderFile()
            val invalidProviderFile = createOriginalProviderFile("invalid_provider")

            val validProvider = testProviderMetadata
            val invalidProvider = webViewProviderMetadata.copy(
                buildUrl = "https://invalid-url.com/provider.flx",
            )

            // Setup valid provider
            every { mockProviderRepository.getProvider(validProvider.id) } returns mockProvider
            coEvery {
                mockGetProviderFromRemoteUseCase(any(), validProvider.id)
            } returns Resource.Success(validProvider.copy(versionCode = 10000L))
            coEvery { mockUnloadProviderUseCase(validProvider, false) } just runs
            coEvery { mockLoadProviderUseCase(any(), any()) } returns flowOf(LoadProviderResult.Success(validProvider))

            // Setup invalid provider with download failure
            val mockInvalidProvider = mockk<Provider>(relaxed = true)
            every { mockProviderRepository.getProvider(invalidProvider.id) } returns mockInvalidProvider
            coEvery {
                mockGetProviderFromRemoteUseCase(any(), invalidProvider.id)
            } returns Resource.Success(invalidProvider.copy(versionCode = 10000L))

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

            // This should throw IllegalStateException due to download failure for invalid provider
            val result = updateProviderUseCase(providers)

            expectThat(result) {
                get { success }.isNotEmpty()
                get { failed }.isNotEmpty() and {
                    get { first().second?.cause }.isA<DownloadFailed>()
                }
            }
        }

    @Test
    fun shouldHandleUnloadFailuresCorrectly() =
        runTest(testDispatcher) {
            val providerFile = createOriginalProviderFile()

            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider
            setupProviderPreferences(testProviderMetadata, providerFile)

            coEvery {
                mockGetProviderFromRemoteUseCase(any(), testProviderMetadata.id)
            } returns Resource.Success(testProviderMetadata.copy(versionCode = 10000L))

            // Make unload fail
            coEvery {
                mockUnloadProviderUseCase(testProviderMetadata, false)
            } throws RuntimeException("Unload failed")

            val result = updateProviderUseCase(listOf(testProviderMetadata))

            expectThat(result) {
                get { success }.isEmpty()
                get { failed }.isNotEmpty() and {
                    get { first().second?.cause }.isA<RuntimeException>()
                }
            }
        }

    @Test
    fun shouldHandleLoadFailuresCorrectlyInBatchUpdate() =
        runTest(testDispatcher) {
            val providerFile = createOriginalProviderFile()

            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider andThen null
            setupProviderPreferences(testProviderMetadata, providerFile)

            coEvery {
                mockGetProviderFromRemoteUseCase(any(), testProviderMetadata.id)
            } returns Resource.Success(testProviderMetadata.copy(versionCode = 10000L))

            coEvery { mockUnloadProviderUseCase(testProviderMetadata, false) } just runs

            // Make load fail
            coEvery {
                mockLoadProviderUseCase(any(), any())
            } returns flowOf(
                LoadProviderResult.Failure(
                    provider = testProviderMetadata,
                    filePath = providerFile.absolutePath,
                    error = RuntimeException("Load failed"),
                ),
            )

            val result = updateProviderUseCase(listOf(testProviderMetadata))

            expectThat(result) {
                get { success }.isEmpty()
                get { failed }.isNotEmpty() and {
                    get { first().second?.cause }.isA<RuntimeException>()
                }
            }
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

            val result = runCatching { updateProviderUseCase(testProviderMetadata) }

            expectThat(result.isSuccess).isTrue()
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

        coEvery { mockUnloadProviderUseCase(testProviderMetadata, false) } just runs
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
