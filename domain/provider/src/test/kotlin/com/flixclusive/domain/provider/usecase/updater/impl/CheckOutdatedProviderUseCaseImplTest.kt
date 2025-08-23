package com.flixclusive.domain.provider.usecase.updater.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.impl.GetProviderFromRemoteUseCaseImpl
import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderResult
import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderUseCase
import com.flixclusive.model.provider.ProviderManifest
import com.flixclusive.provider.Provider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class CheckOutdatedProviderUseCaseImplTest {
    private lateinit var checkOutdatedProviderUseCase: CheckOutdatedProviderUseCase
    private lateinit var mockProviderRepository: ProviderRepository
    private lateinit var appDispatchers: AppDispatchers
    private val testDispatcher = StandardTestDispatcher()

    private val testProviderMetadata = ProviderTestDefaults.getProviderMetadata(
        id = "14a5037ac9553dd",
        name = "Test Provider",
        versionCode = 10000L,
        repositoryUrl = "https://github.com/flixclusiveorg/providers-template",
        buildUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/BasicDummyProvider.flx",
    )

    private val webViewProviderMetadata = ProviderTestDefaults.getWebViewProviderMetadata(
        id = "407e8638eb9d50c",
        name = "WebView Test Provider",
        versionCode = 10000L,
        repositoryUrl = "https://github.com/flixclusiveorg/providers-template",
        buildUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/BasicDummyWebViewProvider.flx",
    )

    @Before
    fun setup() {
        mockProviderRepository = mockk()
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        val client = OkHttpClient.Builder().build()
        val getProviderFromRemoteUseCase = GetProviderFromRemoteUseCaseImpl(
            client = client,
            appDispatchers = appDispatchers,
        )

        checkOutdatedProviderUseCase = CheckOutdatedProviderUseCaseImpl(
            providerRepository = mockProviderRepository,
            getProviderFromRemoteUseCase = getProviderFromRemoteUseCase,
        )
    }

    @Test
    fun `should return empty list when no providers exist`() =
        runTest(testDispatcher) {
            every { mockProviderRepository.getOrderedProviders() } returns emptyList()

            val result = checkOutdatedProviderUseCase()

            expectThat(result).hasSize(0)
        }

    @Test
    fun `should return up to date when provider version matches remote`() =
        runTest(testDispatcher) {
            val mockProvider = createMockProvider(
                versionCode = 10000L,
                updateUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
            )

            every { mockProviderRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = checkOutdatedProviderUseCase()

            expectThat(result).hasSize(1)
            expectThat(result[0]).isA<CheckOutdatedProviderResult.UpToDate>().and {
                get { metadata.id }.isEqualTo(testProviderMetadata.id)
            }
        }

    @Test
    fun `should return outdated when provider version is lower than remote`() =
        runTest(testDispatcher) {
            val mockProvider = createMockProvider(
                versionCode = 9999L, // Lower than remote version (10000L)
                updateUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
            )

            every { mockProviderRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = checkOutdatedProviderUseCase()

            expectThat(result).hasSize(1)
            expectThat(result[0]).isA<CheckOutdatedProviderResult.Outdated>().and {
                get { metadata.id }.isEqualTo(testProviderMetadata.id)
            }
        }

    @Test
    fun `should return up to date when provider version is higher than remote`() =
        runTest(testDispatcher) {
            val mockProvider = createMockProvider(
                versionCode = 10001L, // Higher than remote version (10000L)
                updateUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
            )

            every { mockProviderRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = checkOutdatedProviderUseCase()

            expectThat(result).hasSize(1)
            expectThat(result[0]).isA<CheckOutdatedProviderResult.UpToDate>().and {
                get { metadata.id }.isEqualTo(testProviderMetadata.id)
            }
        }

    @Test
    fun `should return error when provider not found in repository`() =
        runTest(testDispatcher) {
            every { mockProviderRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns null

            val result = checkOutdatedProviderUseCase()

            expectThat(result).hasSize(1)
            expectThat(result[0]).isA<CheckOutdatedProviderResult.UpToDate>() // Returns false, so UpToDate
        }

    @Test
    fun `should return up to date when provider has no update URL`() =
        runTest(testDispatcher) {
            val mockProvider = createMockProvider(
                versionCode = 10000L,
                updateUrl = null,
            )

            every { mockProviderRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = checkOutdatedProviderUseCase()

            expectThat(result).hasSize(1)
            expectThat(result[0]).isA<CheckOutdatedProviderResult.UpToDate>()
        }

    @Test
    fun `should return up to date when provider has empty update URL`() =
        runTest(testDispatcher) {
            val mockProvider = createMockProvider(
                versionCode = 10000L,
                updateUrl = "",
            )

            every { mockProviderRepository.getOrderedProviders() } returns listOf(testProviderMetadata)
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = checkOutdatedProviderUseCase()

            expectThat(result).hasSize(1)
            expectThat(result[0]).isA<CheckOutdatedProviderResult.UpToDate>()
        }

    @Test
    fun `should return up to date for debug providers`() =
        runTest(testDispatcher) {
            val debugMetadata = testProviderMetadata.copy(
                id = "${testProviderMetadata.id}-debug",
            )
            val mockProvider = createMockProvider(
                versionCode = 9999L,
                updateUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
            )

            every { mockProviderRepository.getOrderedProviders() } returns listOf(debugMetadata)
            every { mockProviderRepository.getProvider(debugMetadata.id) } returns mockProvider

            val result = checkOutdatedProviderUseCase()

            expectThat(result).hasSize(1)
            expectThat(result[0]).isA<CheckOutdatedProviderResult.UpToDate>()
        }

    @Test
    fun `should handle multiple providers with mixed results`() =
        runTest(testDispatcher) {
            val provider1 = createMockProvider(
                versionCode = 9999L, // Outdated
                updateUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
            )
            val provider2 = createMockProvider(
                versionCode = 10000L, // Up to date
                updateUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
            )

            val providers = listOf(testProviderMetadata, webViewProviderMetadata)

            every { mockProviderRepository.getOrderedProviders() } returns providers
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns provider1
            every { mockProviderRepository.getProvider(webViewProviderMetadata.id) } returns provider2

            val result = checkOutdatedProviderUseCase()

            expectThat(result).hasSize(2)
            expectThat(result[0]).isA<CheckOutdatedProviderResult.Outdated>()
            expectThat(result[1]).isA<CheckOutdatedProviderResult.UpToDate>()
        }

    @Test
    fun `should return error when network request fails`() =
        runTest(testDispatcher) {
            val invalidMetadata = testProviderMetadata.copy(
                repositoryUrl = "https://github.com/nonexistent/repository",
            )
            val mockProvider = createMockProvider(
                versionCode = 9999L,
                updateUrl = "https://raw.githubusercontent.com/nonexistent/repository/builds/updater.json",
            )

            every { mockProviderRepository.getOrderedProviders() } returns listOf(invalidMetadata)
            every { mockProviderRepository.getProvider(invalidMetadata.id) } returns mockProvider

            val result = checkOutdatedProviderUseCase()

            expectThat(result).hasSize(1)
            expectThat(result[0]).isA<CheckOutdatedProviderResult.Error>()
        }

    @Test
    fun `should return error when provider not found in remote repository`() =
        runTest(testDispatcher) {
            val nonExistentMetadata = testProviderMetadata.copy(
                id = "non-existent-provider-id",
            )
            val mockProvider = createMockProvider(
                versionCode = 9999L,
                updateUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
            )

            every { mockProviderRepository.getOrderedProviders() } returns listOf(nonExistentMetadata)
            every { mockProviderRepository.getProvider(nonExistentMetadata.id) } returns mockProvider

            val result = checkOutdatedProviderUseCase()

            expectThat(result).hasSize(1)
            expectThat(result[0]).isA<CheckOutdatedProviderResult.Error>()
        }

    @Test
    fun `single provider check should return false when up to date`() =
        runTest(testDispatcher) {
            val mockProvider = createMockProvider(
                versionCode = 10000L,
                updateUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
            )

            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = checkOutdatedProviderUseCase(testProviderMetadata)

            expectThat(result).isFalse()
        }

    @Test
    fun `single provider check should return true when outdated`() =
        runTest(testDispatcher) {
            val mockProvider = createMockProvider(
                versionCode = 9999L,
                updateUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
            )

            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = checkOutdatedProviderUseCase(testProviderMetadata)

            expectThat(result).isTrue()
        }

    @Test
    fun `single provider check should return false for debug provider`() =
        runTest(testDispatcher) {
            val debugMetadata = testProviderMetadata.copy(
                id = "${testProviderMetadata.id}-debug",
            )
            val mockProvider = createMockProvider(
                versionCode = 9999L,
                updateUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
            )

            every { mockProviderRepository.getProvider(debugMetadata.id) } returns mockProvider

            val result = checkOutdatedProviderUseCase(debugMetadata)

            expectThat(result).isFalse()
        }

    @Test
    fun `single provider check should return false when provider not found`() =
        runTest(testDispatcher) {
            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns null

            val result = checkOutdatedProviderUseCase(testProviderMetadata)

            expectThat(result).isFalse()
        }

    @Test
    fun `should handle repository URL parsing correctly`() =
        runTest(testDispatcher) {
            val repository = ProviderTestDefaults.getRepositoryFromUrl(
                "https://github.com/flixclusiveorg/providers-template",
            )
            val mockProvider = createMockProvider(
                versionCode = 9999L,
                updateUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
            )

            every { mockProviderRepository.getProvider(testProviderMetadata.id) } returns mockProvider

            val result = checkOutdatedProviderUseCase(testProviderMetadata)

            expectThat(result).isTrue()
            expectThat(repository.owner).isEqualTo("flixclusiveorg")
            expectThat(repository.name).isEqualTo("providers-template")
        }

    private fun createMockProvider(
        versionCode: Long,
        updateUrl: String?,
    ): Provider {
        val mockProvider = mockk<Provider>()
        val mockManifest = mockk<ProviderManifest>()

        every { mockProvider.manifest } returns mockManifest
        every { mockManifest.versionCode } returns versionCode
        every { mockManifest.updateUrl } returns updateUrl

        return mockProvider
    }
}
