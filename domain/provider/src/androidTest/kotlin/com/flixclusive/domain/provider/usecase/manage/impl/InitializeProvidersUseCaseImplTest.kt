package com.flixclusive.domain.provider.usecase.manage.impl

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.data.provider.repository.InstalledRepoRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.ProviderResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.io.File

@RunWith(AndroidJUnit4::class)
class InitializeProvidersUseCaseImplTest {
    private lateinit var initializeProvidersUseCase: InitializeProvidersUseCaseImpl
    private lateinit var context: Context
    private lateinit var mockDataStoreManager: DataStoreManager
    private lateinit var mockLoadProviderUseCase: LoadProviderUseCase

    private lateinit var mockProviderRepository: ProviderRepository
    private lateinit var mockInstalledRepoRepository: InstalledRepoRepository
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var dummyProviderFile: File
    private lateinit var userSessionDataStore: UserSessionDataStore
    private val testDispatcher = StandardTestDispatcher()

    private val testProviderMetadata = ProviderTestDefaults.getProviderMetadata(
        buildUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/BasicDummyProvider.flx",
    )

    private lateinit var testInstalledProvider: InstalledProvider

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        dummyProviderFile = getDummyProviderFile()
        mockDataStoreManager = mockk()
        mockLoadProviderUseCase = mockk()
        mockProviderRepository = mockk(relaxed = true)
        mockInstalledRepoRepository = mockk(relaxed = true)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
        userSessionDataStore = mockk {
            coEvery { currentUserId } returns flowOf(DatabaseTestDefaults.TEST_USER_ID)
        }
        testInstalledProvider = DatabaseTestDefaults.getInstalledProvider(
            id = testProviderMetadata.id,
            repositoryUrl = testProviderMetadata.repositoryUrl,
            filePath = dummyProviderFile.absolutePath,
        )

        initializeProvidersUseCase = InitializeProvidersUseCaseImpl(
            context = context,
            loadProviderUseCase = mockLoadProviderUseCase,
            userSessionDataStore = userSessionDataStore,
            providerRepository = mockProviderRepository,
            installedRepoRepository = mockInstalledRepoRepository,
            dataStoreManager = mockDataStoreManager,
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
                any(),
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
    fun shouldInitializeProvidersFromPersistence() =
        runTest(testDispatcher) {
            coEvery {
                mockInstalledRepoRepository.isInstalled(
                    url = testProviderMetadata.repositoryUrl,
                    ownerId = testInstalledProvider.ownerId
                )
            } returns true

            coEvery {
                mockProviderRepository.getInstalledProviders(testInstalledProvider.ownerId)
            } returns listOf(testInstalledProvider)

            coEvery {
                mockLoadProviderUseCase(testInstalledProvider)
            } returns flowOf(ProviderResult.Success(testProviderMetadata))

            initializeProvidersUseCase().test {
                val result = awaitItem()
                expectThat(result).isA<ProviderResult.Success>().and {
                    get { provider.id }.isEqualTo(testProviderMetadata.id)
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
                name = testProviderMetadata.name,
            )

            debugUpdaterFile.writeText(Json.encodeToString(listOf(debugMetadata)))

            val debugProvider = DatabaseTestDefaults.getInstalledProvider(
                id = debugMetadata.id,
                filePath = debugProviderFile.absolutePath,
                isDebug = true,
            )

            val preferencesWithDebugProvider = ProviderPreferences(
                shouldAddDebugPrefix = true,
            )

            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(preferencesWithDebugProvider)

            val expectedDebugMetadata = debugMetadata.copy(
                id = "${debugMetadata.id}-debug",
                name = "${debugMetadata.name}-debug",
            )

            coEvery { mockProviderRepository.getInstalledProviders(debugProvider.ownerId) } returns listOf(debugProvider)

            coEvery {
                mockLoadProviderUseCase(debugProvider)
            } returns flowOf(ProviderResult.Success(expectedDebugMetadata))

            initializeProvidersUseCase().test {
                val result = awaitItem()
                expectThat(result).isA<ProviderResult.Success>().and {
                    get { provider.id }.isEqualTo("${debugMetadata.id}-debug")
                    get { provider.name }.isEqualTo("${debugMetadata.name}-debug")
                }
                awaitComplete()
            }

            // Clean up debug directory
            debugDir.parentFile?.deleteRecursively()
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
