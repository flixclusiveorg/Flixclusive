package com.flixclusive.domain.provider.usecase.manage.impl

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderFromPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.data.downloads.model.DownloadState
import com.flixclusive.data.downloads.model.DownloadStatus
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.downloads.usecase.DownloadFileUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.io.File
import java.io.FileNotFoundException

@RunWith(AndroidJUnit4::class)
class LoadProviderUseCaseImplTest {
    private lateinit var loadProviderUseCase: LoadProviderUseCaseImpl
    private lateinit var context: Context
    private lateinit var mockUserSessionDataStore: UserSessionDataStore
    private lateinit var mockDataStoreManager: DataStoreManager
    private lateinit var mockProviderRepository: ProviderRepository
    private lateinit var mockProviderApiRepository: ProviderApiRepository
    private lateinit var downloadFileUseCase: DownloadFileUseCase
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var dummyProviderFile: File
    private val testDispatcher = StandardTestDispatcher()

    private val testUserId = 1
    private val testProviderMetadata = ProviderTestDefaults.getProviderMetadata(
        buildUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/BasicDummyProvider.flx",
    )

    private val DownloadState.Companion.ERROR
        get() = DownloadState(
            downloadId = "",
            status = DownloadStatus.FAILED,
            error = UiText.from("Error"),
        )

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        dummyProviderFile = getDummyProviderFile()
        mockUserSessionDataStore = mockk()
        mockDataStoreManager = mockk()
        mockProviderRepository = mockk(relaxed = true)
        mockProviderApiRepository = mockk(relaxed = true)
        downloadFileUseCase = mockk(relaxed = true)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        loadProviderUseCase = LoadProviderUseCaseImpl(
            context = context,
            userSessionDataStore = mockUserSessionDataStore,
            dataStoreManager = mockDataStoreManager,
            providerRepository = mockProviderRepository,
            providerApiRepository = mockProviderApiRepository,
            appDispatchers = appDispatchers,
            downloadFile = downloadFileUseCase,
        )

        every { mockUserSessionDataStore.currentUserId } returns flowOf(testUserId)

        coEvery {
            mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
        } returns flowOf(ProviderPreferences())

        every { mockProviderRepository.getProvider(any()) } returns null
    }

    @After
    fun tearDown() {
        // Clean up the dummy provider file after tests
        if (dummyProviderFile.exists()) {
            dummyProviderFile.delete()
        }
    }

    @Test
    fun shouldFailToLoadProviderWhenBuildUrlIsInvalid() =
        runTest(testDispatcher) {
            val invalidMetadata = testProviderMetadata.copy(
                buildUrl = "https://invalid-domain-that-does-not-exist-12345.com/invalid.flx",
            )

            every { downloadFileUseCase.invoke(any()) } returns flowOf(DownloadState.ERROR)

            loadProviderUseCase(invalidMetadata).test {
                val result = awaitItem()
                expectThat(result).isA<LoadProviderResult.Failure>().and {
                    get { provider.id }.isEqualTo(invalidMetadata.id)
                    get { isFileDownloaded }.isEqualTo(false)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun shouldSkipLoadingWhenProviderAlreadyExists() =
        runTest(testDispatcher) {
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockk()

            loadProviderUseCase(testProviderMetadata).test {
                expectThat(awaitItem()).isA<LoadProviderResult.Failure>().and {
                    get { provider.id }.isEqualTo(testProviderMetadata.id)
                    get { error }.isA<IllegalStateException>()
                }

                awaitComplete()
            }
        }

    @Test
    fun shouldLoadProviderFromProviderFile() =
        runTest(testDispatcher) {
            loadProviderUseCase(
                metadata = testProviderMetadata,
                filePath = dummyProviderFile.absolutePath,
            ).test {
                val result = awaitItem()
                expectThat(result).isA<LoadProviderResult.Success>().and {
                    get { provider.id }.isEqualTo(testProviderMetadata.id)
                }
                awaitComplete()
            }
        }

    @Test
    fun shouldFailWhenFileDoesNotExist() =
        runTest(testDispatcher) {
            val nonExistentFilePath = "src/androidTest/assets/non-existent.flx"

            loadProviderUseCase(
                metadata = testProviderMetadata,
                filePath = nonExistentFilePath,
            ).test {
                expectThat(awaitItem()).isA<LoadProviderResult.Failure>().and {
                    get { provider.id }.isEqualTo(testProviderMetadata.id)
                    get { error }.isA<FileNotFoundException>()
                }

                awaitComplete()
            }
        }

    @Test
    fun shouldHandleProviderPreferencesCorrectly() =
        runTest(testDispatcher) {
            val existingProvider = ProviderFromPreferences(
                id = testProviderMetadata.id,
                name = "Basic Dummy Provider",
                filePath = "src/androidTest/assets/BasicDummyProvider.flx",
                isDisabled = true,
            )
            val preferences = ProviderPreferences(providers = listOf(existingProvider))

            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(preferences)

            loadProviderUseCase(
                metadata = testProviderMetadata,
                filePath = dummyProviderFile.absolutePath,
            ).test {
                val result = awaitItem()
                expectThat(result).isA<LoadProviderResult.Success>()
                awaitComplete()
            }
        }

    @Test
    fun shouldValidateProviderManifestFromProviderFile() =
        runTest(testDispatcher) {
            loadProviderUseCase(
                metadata = testProviderMetadata.copy(
                    id = "BasicDummyProvider",
                    name = "Basic Dummy Provider",
                ),
                filePath = dummyProviderFile.absolutePath,
            ).test {
                val result = awaitItem()
                expectThat(result).isA<LoadProviderResult.Success>().and {
                    get { provider.name }.isEqualTo("Basic Dummy Provider")
                }
                awaitComplete()
            }
        }

    @Test
    fun shouldHandleRepositoryUrlParsing() {
        val repository = ProviderTestDefaults.getRepositoryFromUrl()
        val rawLink = repository.getRawLink("updater.json", "builds")

        expectThat(repository.owner).isEqualTo("flixclusiveorg")
        expectThat(repository.name).isEqualTo("providers-template")
        expectThat(rawLink).isEqualTo(
            "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
        )
    }

    @Test
    fun shouldFailGracefullyWithNetworkErrors() =
        runTest(testDispatcher) {
            val invalidMetadata = testProviderMetadata.copy(
                buildUrl = "https://httpstat.us/500",
            )

            loadProviderUseCase(invalidMetadata).test {
                val result = awaitItem()
                expectThat(result).isA<LoadProviderResult.Failure>().and {
                    get { provider }.isEqualTo(invalidMetadata)
                }
                awaitComplete()
            }
        }

    private fun getDummyProviderFile(): File {
        val assetManager = context.assets
        val tempFile = File(context.cacheDir, "BasicDummyProvider.flx")

        assetManager.open("BasicDummyProvider.flx").use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return tempFile
    }
}
