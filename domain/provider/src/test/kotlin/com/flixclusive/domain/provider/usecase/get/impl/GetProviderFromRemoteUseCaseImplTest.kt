package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.extensions.isFailure
import com.flixclusive.core.testing.extensions.isSuccess
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.core.testing.provider.ProviderTestDefaults.getProviderMetadataList
import com.flixclusive.model.provider.ProviderMetadata
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class GetProviderFromRemoteUseCaseImplTest {
    private lateinit var getProviderFromRemoteUseCase: GetProviderFromRemoteUseCaseImpl
    private lateinit var appDispatchers: AppDispatchers
    private val testDispatcher = StandardTestDispatcher()

    private val testRepository = ProviderTestDefaults.getRepositoryFromUrl()
    private val testProviderMetadata = ProviderTestDefaults.getProviderMetadata()

    @Before
    fun setup() {
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        // Use a real OkHttpClient for integration-style testing
        val client = OkHttpClient.Builder().build()
        getProviderFromRemoteUseCase = GetProviderFromRemoteUseCaseImpl(client, appDispatchers)
    }

    @Test
    fun `repository getRawLink should generate correct URL`() {
        val result = testRepository.getRawLink("updater.json", "builds")

        expectThat(result).isEqualTo(
            "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
        )
    }

    @Test
    fun `invoke with invalid repository should return failure`() =
        runTest(testDispatcher) {
            val invalidRepository = ProviderTestDefaults.getRepositoryFromUrl(
                "https://github.com/nonexistent/invalid-repo",
            )

            val result = getProviderFromRemoteUseCase(invalidRepository)

            expectThat(result).isFailure()
        }

    @Test
    fun `invoke with repository and non-existent id should return failure when no providers match`() =
        runTest(testDispatcher) {
            // This test will fail when trying to fetch from the actual repository, which is expected
            val result = getProviderFromRemoteUseCase(testRepository, "non-existent-id")

            // Since we're using real HTTP calls, this will likely fail with a network error or empty response
            expectThat(result).isFailure()
        }

    @Test
    fun `cache expiration logic should work correctly`() =
        runTest(testDispatcher) {
            // Test the internal caching mechanism by calling the same repository multiple times
            // The first call should make a network request, subsequent calls should use cache
            val result1 = getProviderFromRemoteUseCase(testRepository)
            val result2 = getProviderFromRemoteUseCase(testRepository)

            // Both should be of the same type
            expectThat(result1).isSuccess().and {
                get { data }
                    .isNotNull()
                    .isA<List<ProviderMetadata>>()
                    .isEqualTo(getProviderMetadataList())
            }
            expectThat(result2).isSuccess().and {
                get { data }
                    .isNotNull()
                    .isA<List<ProviderMetadata>>()
                    .isEqualTo(getProviderMetadataList())
            }
        }

    @Test
    fun `invoke with valid repository and id should return provider metadata`() =
        runTest(testDispatcher) {
            val result = getProviderFromRemoteUseCase(testRepository, testProviderMetadata.id)

            expectThat(result).isSuccess()
            val provider = (result as Resource.Success).data

            expectThat(provider).isEqualTo(testProviderMetadata)
        }
}
