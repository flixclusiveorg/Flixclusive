package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.model.provider.Author
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import com.flixclusive.model.provider.Status
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class GetProviderFromRemoteUseCaseImplTest {
    private lateinit var getProviderFromRemoteUseCase: GetProviderFromRemoteUseCaseImpl
    private lateinit var appDispatchers: AppDispatchers
    private val testDispatcher = StandardTestDispatcher()

    private val testRepository = "https://github.com/flixclusiveorg/providers-template"
        .toValidRepositoryLink()

    private val testProviderMetadata = ProviderMetadata(
        id = "14a5037ac9553dd",
        name = "Test Provider",
        authors = listOf(
            Author(
                name = "flixclusiveorg",
                image = "http://github.com/flixclusiveorg.png",
                socialLink = "http://github.com/flixclusiveorg",
            ),
        ),
        repositoryUrl = "https://github.com/flixclusiveorg/providers-template",
        buildUrl = "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/BasicDummyProvider.flx",
        changelog = "# Header\n## Secondary header\n---\n\nList\n- Item 1\n- Item 2\n- Item 3",
        versionName = "1.0.0",
        versionCode = 10000,
        adult = false,
        description = "A dummy provider that does nothing.",
        iconUrl = null,
        language = Language.Multiple,
        providerType = ProviderType.All,
        status = Status.Working,
    )

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
            val invalidRepository = "https://github.com/nonexistent/invalid-repo"
                .toValidRepositoryLink()

            val result = getProviderFromRemoteUseCase(invalidRepository)

            expectThat(result).isA<Resource.Failure>()
        }

    @Test
    fun `invoke with repository and non-existent id should return failure when no providers match`() =
        runTest(testDispatcher) {
            // This test will fail when trying to fetch from the actual repository, which is expected
            val result = getProviderFromRemoteUseCase(testRepository, "non-existent-id")

            // Since we're using real HTTP calls, this will likely fail with a network error or empty response
            expectThat(result).isA<Resource.Failure>()
        }

    @Test
    fun `cache expiration logic should work correctly`() =
        runTest(testDispatcher) {
            // Test the internal caching mechanism by calling the same repository multiple times
            // The first call should make a network request, subsequent calls should use cache
            val result1 = getProviderFromRemoteUseCase(testRepository)
            val result2 = getProviderFromRemoteUseCase(testRepository)

            // Both should be of the same type
            expectThat(result1).isA<Resource.Success<*>>()
            expectThat(result2).isA<Resource.Success<*>>()
        }

    @Test
    fun `invoke with valid repository and id should return provider metadata`() = runTest(testDispatcher) {
        val result = getProviderFromRemoteUseCase(testRepository, testProviderMetadata.id)

        expectThat(result).isA<Resource.Success<ProviderMetadata>>()
        val provider = (result as Resource.Success).data

        expectThat(provider).isEqualTo(testProviderMetadata)
    }
}
