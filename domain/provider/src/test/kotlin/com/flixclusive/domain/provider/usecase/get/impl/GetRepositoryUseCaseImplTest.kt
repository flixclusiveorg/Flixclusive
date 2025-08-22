package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.extensions.isFailure
import com.flixclusive.core.testing.extensions.isSuccess
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.model.provider.Repository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class GetRepositoryUseCaseImplTest {
    private lateinit var getRepositoryUseCase: GetRepositoryUseCaseImpl
    private lateinit var mockDataStoreManager: DataStoreManager
    private lateinit var appDispatchers: AppDispatchers
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        mockDataStoreManager = mockk()
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        // Use real OkHttpClient for actual network calls
        val client = OkHttpClient.Builder().build()
        getRepositoryUseCase = GetRepositoryUseCaseImpl(client, mockDataStoreManager, appDispatchers)

        // Mock empty provider preferences by default
        coEvery {
            mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
        } returns flowOf(ProviderPreferences(repositories = emptyList()))
    }

    @Test
    fun `invoke with valid GitHub repository URL should return success`() =
        runTest(testDispatcher) {
            val validUrl = "https://github.com/flixclusiveorg/providers-template"

            val result = getRepositoryUseCase(validUrl)

            expectThat(result).isSuccess().and {
                get { data }
                    .isNotNull()
                    .isA<Repository>()
                    .and {
                        get { owner }.isEqualTo("flixclusiveorg")
                        get { name }.isEqualTo("providers-template")
                        get { url }.isEqualTo("https://github.com/flixclusiveorg/providers-template")
                    }
            }
        }

    @Test
    fun `invoke with GitHub URL without protocol should return success`() =
        runTest(testDispatcher) {
            val urlWithoutHttps = "github.com/flixclusiveorg/providers-template"

            val result = getRepositoryUseCase(urlWithoutHttps)

            expectThat(result).isSuccess().and {
                get { data }
                    .isNotNull()
                    .isA<Repository>()
                    .and {
                        get { owner }.isEqualTo("flixclusiveorg")
                        get { name }.isEqualTo("providers-template")
                    }
            }
        }

    @Test
    fun `invoke with GitHub URL with http should return success`() =
        runTest(testDispatcher) {
            val urlWithoutHttps = "http://github.com/flixclusiveorg/providers-template"

            val result = getRepositoryUseCase(urlWithoutHttps)

            expectThat(result).isSuccess().and {
                get { data }
                    .isNotNull()
                    .isA<Repository>()
                    .and {
                        get { owner }.isEqualTo("flixclusiveorg")
                        get { name }.isEqualTo("providers-template")
                    }
            }
        }

    @Test
    fun `invoke with GitHub URL with https should return success`() =
        runTest(testDispatcher) {
            val urlWithoutHttps = "https://github.com/flixclusiveorg/providers-template"

            val result = getRepositoryUseCase(urlWithoutHttps)

            expectThat(result).isSuccess().and {
                get { data }
                    .isNotNull()
                    .isA<Repository>()
                    .and {
                        get { owner }.isEqualTo("flixclusiveorg")
                        get { name }.isEqualTo("providers-template")
                    }
            }
        }

    @Test
    fun `invoke with invalid repository URL should return failure`() =
        runTest(testDispatcher) {
            val invalidUrl = "not-a-valid-url"

            val result = getRepositoryUseCase(invalidUrl)

            expectThat(result).isFailure()
        }

    @Test
    fun `invoke with non-existent repository should return failure`() =
        runTest(testDispatcher) {
            val nonExistentUrl = "https://github.com/nonexistent/invalid-repo"

            val result = getRepositoryUseCase(nonExistentUrl)

            expectThat(result).isFailure()
        }

    @Test
    fun `invoke with already added repository should return failure`() =
        runTest(testDispatcher) {
            val existingRepository = ProviderTestDefaults.getRepository()
            val existingPreferences = ProviderPreferences(repositories = listOf(existingRepository))

            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(existingPreferences)

            val result = getRepositoryUseCase("https://github.com/flixclusiveorg/providers-template")

            expectThat(result).isFailure()
        }

    @Test
    fun `invoke with repository having different case should detect as already added`() =
        runTest(testDispatcher) {
            val existingRepository = ProviderTestDefaults.getRepository()
            val existingPreferences = ProviderPreferences(repositories = listOf(existingRepository))

            coEvery {
                mockDataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            } returns flowOf(existingPreferences)

            // Use different case for owner name
            val result = getRepositoryUseCase("https://github.com/FLIXCLUSIVEORG/providers-template")

            expectThat(result).isFailure()
        }

    @Test
    fun `repository URL parsing should work with different formats`() =
        runTest(testDispatcher) {
            val testCases = listOf(
                "github.com/flixclusiveorg/providers-template",
                "https://github.com/flixclusiveorg/providers-template",
                "https://github.com/flixclusiveorg/providers-template/",
                "flixclusiveorg/providers-template",
            )

            testCases.forEach { url ->
                val result = getRepositoryUseCase(url)
                if (result is Resource.Success) {
                    expectThat(result.data).isNotNull().isA<Repository>().and {
                        get { owner }.isEqualTo("flixclusiveorg")
                        get { name }.isEqualTo("providers-template")
                    }
                }
            }
        }

    @Test
    fun `invoke should handle network exceptions gracefully`() =
        runTest(testDispatcher) {
            // Using a malformed URL that would cause an exception
            val malformedUrl = "https://invalid-domain-that-does-not-exist-12345.com/user/repo"

            val result = getRepositoryUseCase(malformedUrl)

            expectThat(result).isFailure()
        }

    @Test
    fun `raw link generation should work correctly for different repository types`() {
        val githubRepo = ProviderTestDefaults.getRepository()
        val gitlabRepo = ProviderTestDefaults.getRepository(
            owner = "testuser",
            name = "testrepo",
            url = "https://gitlab.com/testuser/testrepo",
            rawLinkFormat = "https://gitlab.com/testuser/testrepo/-/raw/%branch%/%filename%",
        )

        val githubRawLink = githubRepo.getRawLink("updater.json", "builds")
        val gitlabRawLink = gitlabRepo.getRawLink("updater.json", "builds")

        expectThat(githubRawLink).isEqualTo(
            "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/updater.json",
        )
        expectThat(gitlabRawLink).isEqualTo(
            "https://gitlab.com/testuser/testrepo/-/raw/builds/updater.json",
        )
    }

    // TODO: Add more tests for edge cases, such as: GitLab repositories, Codeberg repositories,
    //       handling of different branch names, etc.
}
