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
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.ProviderResult
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
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var dummyProviderFile: File
    private lateinit var testInstalledProvider: InstalledProvider
    private val testDispatcher = StandardTestDispatcher()

    private val testUserId = DatabaseTestDefaults.TEST_USER_ID
    private val testProviderMetadata = ProviderTestDefaults.getProviderMetadata(
        buildUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/BasicDummyProvider.flx",
    )

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        mockUserSessionDataStore = mockk()
        mockDataStoreManager = mockk()
        mockProviderRepository = mockk(relaxed = true)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
        dummyProviderFile = getDummyProviderFile()
        testInstalledProvider = DatabaseTestDefaults.getInstalledProvider(
            id = testProviderMetadata.id,
            repositoryUrl = testProviderMetadata.repositoryUrl,
            filePath = dummyProviderFile.absolutePath,
        )

        loadProviderUseCase = LoadProviderUseCaseImpl(
            context = context,
            userSessionDataStore = mockUserSessionDataStore,
            dataStoreManager = mockDataStoreManager,
            providerRepository = mockProviderRepository,
            appDispatchers = appDispatchers,
        )

        every { mockUserSessionDataStore.currentUserId } returns flowOf(testUserId)

        coEvery {
            mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
        } returns flowOf(ProviderPreferences())

        every { mockProviderRepository.getPlugin(any()) } returns null
    }

    @After
    fun tearDown() {
        if (dummyProviderFile.exists()) {
            dummyProviderFile.delete()
        }
    }

    @Test
    fun shouldSkipLoadingWhenProviderAlreadyExists() =
        runTest(testDispatcher) {
            every { mockProviderRepository.getMetadata(testProviderMetadata.id) } returns testProviderMetadata
            every { mockProviderRepository.getPlugin(testProviderMetadata.id) } returns mockk()

            loadProviderUseCase(testInstalledProvider).test {
                expectThat(awaitItem()).isA<ProviderResult.Failure>().and {
                    get { provider.id }.isEqualTo(testProviderMetadata.id)
                    get { error }.isA<IllegalStateException>()
                }

                awaitComplete()
            }
        }

    @Test
    fun shouldLoadProvider() =
        runTest(testDispatcher) {
            every { mockProviderRepository.getMetadata(testProviderMetadata.id) } returns testProviderMetadata
            loadProviderUseCase(testInstalledProvider).test {
                val result = awaitItem()
                expectThat(result).isA<ProviderResult.Success>().and {
                    get { provider.id }.isEqualTo(testProviderMetadata.id)
                }
                awaitComplete()
            }
        }

    @Test
    fun shouldFailWhenFileDoesNotExist() =
        runTest(testDispatcher) {
            val nonExistentFilePath = "src/androidTest/assets/non-existent.flx"
            val tempProvider = testInstalledProvider.copy(
                filePath = nonExistentFilePath,
            )

            every { mockProviderRepository.getMetadata(testProviderMetadata.id) } returns testProviderMetadata
            loadProviderUseCase(tempProvider).test {
                expectThat(awaitItem()).isA<ProviderResult.Failure>().and {
                    get { provider.id }.isEqualTo(testProviderMetadata.id)
                    get { error }.isA<FileNotFoundException>()
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
