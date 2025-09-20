package com.flixclusive.domain.provider.usecase.manage.impl

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderFromPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.provider.Provider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import strikt.assertions.isA
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File

@RunWith(AndroidJUnit4::class)
class UnloadProviderUseCaseImplTest {
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var unloadProviderUseCase: UnloadProviderUseCaseImpl
    private lateinit var context: Context
    private lateinit var mockDataStoreManager: DataStoreManager
    private lateinit var mockProviderRepository: ProviderRepository
    private lateinit var mockProviderApiRepository: ProviderApiRepository
    private lateinit var mockProvider: Provider
    private lateinit var dummyProviderFile: File
    private lateinit var tempDirectory: File
    private val testDispatcher = StandardTestDispatcher()

    private val buildUrlPrefix = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds"
    private val testProviderMetadata = ProviderTestDefaults.getProviderMetadata(
        buildUrl = "$buildUrlPrefix/BasicDummyProvider.flx",
    )
    private val webViewProviderMetadata = ProviderTestDefaults.getWebViewProviderMetadata(
        buildUrl = "$buildUrlPrefix/BasicDummyWebViewProvider.flx",
    )

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        mockDataStoreManager = mockk()
        mockProviderRepository = mockk(relaxed = true)
        mockProviderApiRepository = mockk(relaxed = true)
        mockProvider = mockk(relaxed = true)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        dummyProviderFile = createDummyProviderFile()

        unloadProviderUseCase = UnloadProviderUseCaseImpl(
            context = context,
            dataStoreManager = mockDataStoreManager,
            providerRepository = mockProviderRepository,
            providerApiRepository = mockProviderApiRepository,
            appDispatchers = appDispatchers,
        )

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
    }

    @After
    fun tearDown() {
        // Clean up temp directory
        if (tempDirectory.exists()) {
            tempDirectory.deleteRecursively()
        }
    }

    @Test
    fun shouldSuccessfullyUnloadProvider() =
        runTest(testDispatcher) {
            val providerFromPrefs = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = dummyProviderFile.absolutePath,
                isDisabled = false,
            )

            setupProviderPreferences(listOf(providerFromPrefs))
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = runCatching {
                unloadProviderUseCase(testProviderMetadata)
            }

            expectThat(result.isSuccess).isTrue()
            verify { mockProvider.onUnload(context) }
            coVerify { mockProviderRepository.remove(testProviderMetadata.id) }
            coVerify { mockProviderApiRepository.removeApi(testProviderMetadata.id) }
            coVerify { mockProviderRepository.removeFromPreferences(testProviderMetadata.id) }
        }

    @Test
    fun shouldUnloadProviderWithoutRemovingFromPreferences() =
        runTest(testDispatcher) {
            val providerFromPrefs = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = dummyProviderFile.absolutePath,
                isDisabled = false,
            )

            setupProviderPreferences(listOf(providerFromPrefs))
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = runCatching {
                unloadProviderUseCase(
                    metadata = testProviderMetadata,
                    unloadFromPrefs = false,
                )
            }

            expectThat(result.isSuccess).isTrue()
            verify { mockProvider.onUnload(context) }
            coVerify { mockProviderRepository.remove(testProviderMetadata.id) }
            coVerify { mockProviderApiRepository.removeApi(testProviderMetadata.id) }
            coVerify(exactly = 0) { mockProviderRepository.removeFromPreferences(any()) }
        }

    @Test
    fun shouldFailWhenProviderNotFoundInPreferences() =
        runTest(testDispatcher) {
            setupProviderPreferences(emptyList())

            val result = runCatching {
                unloadProviderUseCase(testProviderMetadata)
            }

            expectThat(result.isFailure).isTrue()
            expectThat(result.exceptionOrNull()).isA<IllegalStateException>()
        }

    @Test
    fun shouldFailWhenProviderFileDoesNotExist() =
        runTest(testDispatcher) {
            val providerFromPrefs = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = "/non/existent/path/provider.flx",
                isDisabled = false,
            )

            setupProviderPreferences(listOf(providerFromPrefs))
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = runCatching {
                unloadProviderUseCase(testProviderMetadata)
            }

            expectThat(result.isFailure).isTrue()
            coVerify(exactly = 0) { mockProviderRepository.remove(any()) }
            coVerify(exactly = 0) { mockProviderApiRepository.removeApi(any()) }
        }

    @Test
    fun shouldFailWhenProviderNotLoadedInRepository() =
        runTest(testDispatcher) {
            val providerFromPrefs = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = dummyProviderFile.absolutePath,
                isDisabled = false,
            )

            setupProviderPreferences(listOf(providerFromPrefs))
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns null

            val result = kotlin.runCatching {
                unloadProviderUseCase(testProviderMetadata)
            }

            expectThat(result.isFailure).isTrue()
            coVerify(exactly = 0) { mockProviderRepository.remove(any()) }
            coVerify(exactly = 0) { mockProviderApiRepository.removeApi(any()) }
        }

    @Test
    fun shouldUnloadMultipleProvidersSequentially() =
        runTest(testDispatcher) {
            val providerFile1 = createProviderFileInSeparateDirectory("provider1")
            val providerFile2 = createProviderFileInSeparateDirectory("provider2")

            val provider1FromPrefs = ProviderFromPreferences(
                id = "provider1",
                name = "Provider 1",
                filePath = providerFile1.absolutePath,
                isDisabled = false,
            )

            val provider2FromPrefs = ProviderFromPreferences(
                id = "provider2",
                name = "Provider 2",
                filePath = providerFile2.absolutePath,
                isDisabled = false,
            )

            val mockProvider1 = mockk<Provider>(relaxed = true)
            val mockProvider2 = mockk<Provider>(relaxed = true)

            setupProviderPreferences(listOf(provider1FromPrefs, provider2FromPrefs))

            every { mockProviderRepository.getProvider("provider1") } returns mockProvider1
            every { mockProviderRepository.getProvider("provider2") } returns mockProvider2

            val metadata1 = testProviderMetadata.copy(id = "provider1", name = "Provider 1")
            val metadata2 = webViewProviderMetadata.copy(id = "provider2", name = "Provider 2")

            val result1 = runCatching { unloadProviderUseCase(metadata1) }
            val result2 = runCatching { unloadProviderUseCase(metadata2) }

            expectThat(result1.isSuccess).isTrue()
            expectThat(result2.isSuccess).isTrue()

            verify { mockProvider1.onUnload(context) }
            verify { mockProvider2.onUnload(context) }
            coVerify { mockProviderRepository.remove("provider1") }
            coVerify { mockProviderRepository.remove("provider2") }
        }

    @Test
    fun shouldDeleteUpdaterJsonWhenItIsOnlyRemainingFile() =
        runTest(testDispatcher) {
            val providerDir = File(tempDirectory, "provider_with_updater")
            providerDir.mkdirs()

            val providerFile = File(providerDir, "BasicDummyProvider.flx")
            val updaterFile = File(providerDir, "updater.json")

            // Copy provider file and create updater.json
            context.assets.open("BasicDummyProvider.flx").use { inputStream ->
                providerFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            updaterFile.writeText(Json.encodeToString(listOf(testProviderMetadata)))

            val providerFromPrefs = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = providerFile.absolutePath,
                isDisabled = false,
            )

            setupProviderPreferences(listOf(providerFromPrefs))
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = runCatching { unloadProviderUseCase(testProviderMetadata) }

            expectThat(result.isSuccess).isTrue()
            expectThat(providerFile.exists()).isFalse()
            expectThat(updaterFile.exists()).isFalse()
            expectThat(providerDir.exists()).isFalse()
        }

    @Test
    fun shouldNotDeleteDirectoryWhenMultipleFilesExist() =
        runTest(testDispatcher) {
            val providerDir = File(tempDirectory, "provider_with_multiple_files")
            providerDir.mkdirs()

            val providerFile = File(providerDir, "BasicDummyProvider.flx")
            val updaterFile = File(providerDir, "updater.json")
            val otherFile = File(providerDir, "other.txt")

            // Copy provider file and create other files
            context.assets.open("BasicDummyProvider.flx").use { inputStream ->
                providerFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            updaterFile.writeText(Json.encodeToString(listOf(testProviderMetadata)))
            otherFile.writeText("some other content")

            val providerFromPrefs = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = providerFile.absolutePath,
                isDisabled = false,
            )

            setupProviderPreferences(listOf(providerFromPrefs))
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = runCatching { unloadProviderUseCase(testProviderMetadata) }

            expectThat(result.isSuccess).isTrue()
            expectThat(providerFile.exists()).isFalse()
            expectThat(updaterFile.exists()).isTrue()
            expectThat(otherFile.exists()).isTrue()
            expectThat(providerDir.exists()).isTrue()
        }

    @Test
    fun shouldHandleDebugProviders() =
        runTest(testDispatcher) {
            val debugProviderFromPrefs = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = dummyProviderFile.absolutePath,
                isDisabled = false,
                isDebug = true,
            )

            setupProviderPreferences(listOf(debugProviderFromPrefs))
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = runCatching { unloadProviderUseCase(testProviderMetadata) }

            expectThat(result.isSuccess).isTrue()
            verify { mockProvider.onUnload(context) }
            coVerify { mockProviderRepository.remove(testProviderMetadata.id) }
            coVerify { mockProviderApiRepository.removeApi(testProviderMetadata.id) }
            coVerify { mockProviderRepository.removeFromPreferences(testProviderMetadata.id) }
        }

    @Test
    fun shouldHandleProviderOnUnloadException() =
        runTest(testDispatcher) {
            val mockProviderWithException = mockk<Provider>()
            every { mockProviderWithException.onUnload(context) } throws RuntimeException("Unload failed")
            every { mockProviderWithException.name } returns testProviderMetadata.name

            val providerFromPrefs = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = testProviderMetadata.name,
                filePath = dummyProviderFile.absolutePath,
                isDisabled = false,
            )

            setupProviderPreferences(listOf(providerFromPrefs))
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProviderWithException

            val result = runCatching { unloadProviderUseCase(testProviderMetadata) }

            expectThat(result.isFailure).isTrue()
            expectThat(result.exceptionOrNull()?.cause).isA<RuntimeException>()
            verify { mockProviderWithException.onUnload(context) }
        }

    private fun setupProviderPreferences(providers: List<ProviderFromPreferences>) {
        val preferences = ProviderPreferences(providers = providers)
        coEvery {
            mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
        } returns flowOf(preferences)
    }

    private fun createDummyProviderFile(): File {
        tempDirectory = File(context.cacheDir, "test_unload_${System.currentTimeMillis()}")
        tempDirectory.mkdirs()

        val providerFile = File(tempDirectory, "BasicDummyProvider.flx")
        val updaterFile = File(tempDirectory, "updater.json")

        // Copy provider file from assets
        context.assets.open("BasicDummyProvider.flx").use { inputStream ->
            providerFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        // Create updater.json using Kotlin serialization
        updaterFile.writeText(Json.encodeToString(listOf(testProviderMetadata)))

        return providerFile
    }

    private fun createProviderFileInSeparateDirectory(providerName: String): File {
        val providerDir = File(tempDirectory, providerName)
        providerDir.mkdirs()

        val providerFile = File(providerDir, "BasicDummyProvider.flx")
        val updaterFile = File(providerDir, "updater.json")

        // Copy provider file from assets
        context.assets.open("BasicDummyProvider.flx").use { inputStream ->
            providerFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        // Create metadata for this specific provider
        val metadata = if (providerName == "provider2") {
            webViewProviderMetadata.copy(id = providerName, name = providerName.replaceFirstChar { it.uppercase() })
        } else {
            testProviderMetadata.copy(id = providerName, name = providerName.replaceFirstChar { it.uppercase() })
        }

        updaterFile.writeText(Json.encodeToString(listOf(metadata)))

        return providerFile
    }
}
