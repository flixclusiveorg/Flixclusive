package com.flixclusive.domain.provider.usecase.manage.impl

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderFromPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.io.File

@RunWith(AndroidJUnit4::class)
class InitializeProvidersUseCaseImplTest {
    private lateinit var initializeProvidersUseCase: InitializeProvidersUseCaseImpl
    private lateinit var context: Context
    private lateinit var mockDataStoreManager: DataStoreManager
    private lateinit var mockLoadProviderUseCase: LoadProviderUseCase
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var dummyProviderFile: File
    private val testDispatcher = StandardTestDispatcher()

    private val testProviderMetadata = ProviderTestDefaults.getProviderMetadata(
        buildUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/BasicDummyProvider.flx",
    )

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        dummyProviderFile = getDummyProviderFile()
        mockDataStoreManager = mockk()
        mockLoadProviderUseCase = mockk()
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        initializeProvidersUseCase = InitializeProvidersUseCaseImpl(
            context = context,
            dataStoreManager = mockDataStoreManager,
            loadProviderUseCase = mockLoadProviderUseCase,
            appDispatchers = appDispatchers,
        )

        // Setup default mock behavior
        coEvery {
            mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
        } returns flowOf(ProviderPreferences())

        coEvery {
            mockDataStoreManager.updateUserPrefs(
                UserPreferences.PROVIDER_PREFS_KEY,
                ProviderPreferences::class,
                any()
            )
        } returns Unit
    }

    @After
    fun tearDown() {
        // Clean up the entire temporary directory
        val tempDir = dummyProviderFile.parentFile
        if (tempDir?.exists() == true) {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun shouldInitializeEmptyProvidersWhenNoProvidersExist() =
        runTest(testDispatcher) {
            val emptyPreferences = ProviderPreferences(providers = emptyList())
            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(emptyPreferences)

            initializeProvidersUseCase().test {
                awaitComplete()
            }
        }

    @Test
    fun shouldInitializeProvidersFromPreferences() =
        runTest(testDispatcher) {
            val providerFromPrefs = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = dummyProviderFile.absolutePath,
                isDisabled = false,
            )

            val preferencesWithProvider = ProviderPreferences(
                providers = listOf(providerFromPrefs)
            )

            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(preferencesWithProvider)

            coEvery {
                mockLoadProviderUseCase(testProviderMetadata, dummyProviderFile.absolutePath)
            } returns flowOf(LoadProviderResult.Success(testProviderMetadata))

            initializeProvidersUseCase().test {
                val result = awaitItem()
                expectThat(result).isA<LoadProviderResult.Success>().and {
                    get { provider.id }.isEqualTo(testProviderMetadata.id)
                }
                awaitComplete()
            }
        }

    @Test
    fun shouldSkipProvidersWithNonExistentFiles() =
        runTest(testDispatcher) {
            val providerFromPrefs = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = "/non/existent/path/provider.flx",
                isDisabled = false,
            )

            val preferencesWithProvider = ProviderPreferences(
                providers = listOf(providerFromPrefs)
            )

            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(preferencesWithProvider)

            initializeProvidersUseCase().test {
                awaitComplete()
            }
        }

    @Test
    fun shouldInitializeMultipleProviders() =
        runTest(testDispatcher) {
            // Create first provider with proper directory structure
            val tempDir1 = File(context.cacheDir, "test_provider1_${System.currentTimeMillis()}")
            tempDir1.mkdirs()
            val dummyProviderFile1 = File(tempDir1, "BasicDummyProvider.flx")
            val updaterFile1 = File(tempDir1, "updater.json")

            // Create second provider with proper directory structure
            val tempDir2 = File(context.cacheDir, "test_provider2_${System.currentTimeMillis() + 1}")
            tempDir2.mkdirs()
            val dummyProviderFile2 = File(tempDir2, "BasicDummyProvider.flx")
            val updaterFile2 = File(tempDir2, "updater.json")

            // Copy provider files from assets
            context.assets.open("BasicDummyProvider.flx").use { inputStream ->
                dummyProviderFile1.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            context.assets.open("BasicDummyProvider.flx").use { inputStream ->
                dummyProviderFile2.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val metadata1 = testProviderMetadata.copy(id = "provider1", name = "Provider 1")
            val metadata2 = testProviderMetadata.copy(id = "provider2", name = "Provider 2")

            // Create updater.json files using Kotlin serialization
            updaterFile1.writeText(Json.encodeToString(listOf(metadata1)))
            updaterFile2.writeText(Json.encodeToString(listOf(metadata2)))

            val provider1 = ProviderFromPreferences(
                id = "provider1",
                name = "Provider 1",
                filePath = dummyProviderFile1.absolutePath,
                isDisabled = false,
            )

            val provider2 = ProviderFromPreferences(
                id = "provider2",
                name = "Provider 2",
                filePath = dummyProviderFile2.absolutePath,
                isDisabled = false,
            )

            val preferencesWithProviders = ProviderPreferences(
                providers = listOf(provider1, provider2)
            )

            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(preferencesWithProviders)

            coEvery {
                mockLoadProviderUseCase(metadata1, dummyProviderFile1.absolutePath)
            } returns flowOf(LoadProviderResult.Success(metadata1))

            coEvery {
                mockLoadProviderUseCase(metadata2, dummyProviderFile2.absolutePath)
            } returns flowOf(LoadProviderResult.Success(metadata2))

            val results = mutableListOf<LoadProviderResult>()
            initializeProvidersUseCase().test {
                results.add(awaitItem())
                results.add(awaitItem())
                awaitComplete()
            }

            expectThat(results).hasSize(2)
            expectThat(results[0]).isA<LoadProviderResult.Success>().and {
                get { provider.id }.isEqualTo("provider1")
            }
            expectThat(results[1]).isA<LoadProviderResult.Success>().and {
                get { provider.id }.isEqualTo("provider2")
            }

            // Clean up temp directories
            tempDir1.deleteRecursively()
            tempDir2.deleteRecursively()
        }

    @Test
    fun shouldHandleLoadProviderFailures() =
        runTest(testDispatcher) {
            val providerFromPrefs = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = dummyProviderFile.absolutePath,
                isDisabled = false,
            )

            val preferencesWithProvider = ProviderPreferences(
                providers = listOf(providerFromPrefs)
            )

            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(preferencesWithProvider)

            val failure = LoadProviderResult.Failure(
                provider = testProviderMetadata,
                filePath = dummyProviderFile.absolutePath,
                error = RuntimeException("Test error")
            )

            coEvery {
                mockLoadProviderUseCase(testProviderMetadata, dummyProviderFile.absolutePath)
            } returns flowOf(failure)

            initializeProvidersUseCase().test {
                val result = awaitItem()
                expectThat(result).isA<LoadProviderResult.Failure>().and {
                    get { provider.id }.isEqualTo(testProviderMetadata.id)
                    get { error.message }.isEqualTo("Test error")
                }
                awaitComplete()
            }
        }

    @Test
    fun shouldHandleDebugProviders() =
        runTest(testDispatcher) {
            // Create debug provider with proper directory structure
            val debugDir = File(context.getExternalFilesDir(null), "providers/debug/test-repo")
            debugDir.mkdirs()

            val debugProviderFile = File(debugDir, "BasicDummyProvider.flx")
            val debugUpdaterFile = File(debugDir, "updater.json")

            // Copy provider file to debug directory
            context.assets.open("BasicDummyProvider.flx").use { inputStream ->
                debugProviderFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Create updater.json for debug provider using Kotlin serialization
            val debugMetadata = testProviderMetadata.copy(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name
            )

            debugUpdaterFile.writeText(Json.encodeToString(listOf(debugMetadata)))

            val debugProvider = ProviderFromPreferences(
                id = debugMetadata.id,
                name = debugMetadata.name,
                filePath = debugProviderFile.absolutePath,
                isDisabled = false,
                isDebug = true,
            )

            val preferencesWithDebugProvider = ProviderPreferences(
                providers = listOf(debugProvider),
                shouldAddDebugPrefix = true
            )

            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(preferencesWithDebugProvider)

            val expectedDebugMetadata = debugMetadata.copy(
                id = "${debugMetadata.id}-debug",
                name = "${debugMetadata.name}-debug"
            )

            coEvery {
                mockLoadProviderUseCase(expectedDebugMetadata, debugProviderFile.absolutePath)
            } returns flowOf(LoadProviderResult.Success(expectedDebugMetadata))

            initializeProvidersUseCase().test {
                val result = awaitItem()
                expectThat(result).isA<LoadProviderResult.Success>().and {
                    get { provider.id }.isEqualTo("${debugMetadata.id}-debug")
                    get { provider.name }.isEqualTo("${debugMetadata.name}-debug")
                }
                awaitComplete()
            }

            // Clean up debug directory
            debugDir.parentFile?.deleteRecursively()
        }

    @Test
    fun shouldSkipDebugPrefixWhenDisabled() =
        runTest(testDispatcher) {
            val debugProvider = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = dummyProviderFile.absolutePath,
                isDisabled = false,
                isDebug = true,
            )

            val preferencesWithoutDebugPrefix = ProviderPreferences(
                providers = listOf(debugProvider),
                shouldAddDebugPrefix = false
            )

            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(preferencesWithoutDebugPrefix)

            coEvery {
                mockLoadProviderUseCase(testProviderMetadata, dummyProviderFile.absolutePath)
            } returns flowOf(LoadProviderResult.Success(testProviderMetadata))

            initializeProvidersUseCase().test {
                val result = awaitItem()
                expectThat(result).isA<LoadProviderResult.Success>().and {
                    get { provider.id }.isEqualTo(testProviderMetadata.id)
                    get { provider.name }.isEqualTo(testProviderMetadata.name)
                }
                awaitComplete()
            }
        }

    private fun getDummyProviderFile(): File {
        val assetManager = context.assets

        // Create a temporary directory to hold both provider file and updater.json
        val tempDir = File(context.cacheDir, "test_provider_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val tempFile = File(tempDir, "BasicDummyProvider.flx")
        val updaterFile = File(tempDir, "updater.json")

        // Copy the provider file from assets
        assetManager.open("BasicDummyProvider.flx").use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        // Create the updater.json file using Kotlin serialization
        updaterFile.writeText(Json.encodeToString(listOf(testProviderMetadata)))

        return tempFile
    }
}
