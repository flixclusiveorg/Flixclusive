package com.flixclusive.data.provider.repository.impl

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.repository.impl.fake.TestProvider
import com.flixclusive.data.provider.repository.impl.fake.TestProviderApi
import com.flixclusive.data.provider.util.collections.CollectionsOperation
import com.flixclusive.provider.ProviderApi
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

@RunWith(AndroidJUnit4::class)
class ProviderApiRepositoryImplTest {
    private lateinit var repository: ProviderApiRepositoryImpl
    private lateinit var context: Context
    private lateinit var client: OkHttpClient
    private lateinit var providerRepository: ProviderRepository
    private val testDispatcher = StandardTestDispatcher()

    private val testProvider = TestProvider()
    private val testProviderId = "test-provider"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        client = OkHttpClient()
        providerRepository = mockk()

        repository = ProviderApiRepositoryImpl(
            context = context,
            client = client,
            providerRepository = providerRepository,
        )
    }

    @Test
    fun shouldObserveOperations() =
        runTest(testDispatcher) {
            repository.observe().test {
                repository.addApiFromProvider(testProviderId, testProvider)

                val emission = awaitItem()
                expectThat(emission).isA<CollectionsOperation.Map.Add<String, ProviderApi>>()
                expectThat((emission as CollectionsOperation.Map.Add).key).isEqualTo(testProviderId)
                expectThat(emission.value).isA<TestProviderApi>()
            }
        }

    @Test
    fun shouldAddApiFromProvider() =
        runTest(testDispatcher) {
            repository.addApiFromProvider(testProviderId, testProvider)

            val result = repository.getApi(testProviderId)
            expectThat(result).isA<TestProviderApi>()
        }

    @Test
    fun shouldAddApiFromId() =
        runTest(testDispatcher) {
            every { providerRepository.getProvider(testProviderId) } returns testProvider

            repository.addApiFromId(testProviderId)

            val result = repository.getApi(testProviderId)
            expectThat(result).isA<TestProviderApi>()
        }

    @Test
    fun shouldThrowExceptionWhenProviderNotFoundForAddApiFromId() =
        runTest(testDispatcher) {
            every { providerRepository.getProvider(testProviderId) } returns null

            try {
                repository.addApiFromId(testProviderId)
                throw AssertionError("Expected NullPointerException")
            } catch (e: NullPointerException) {
                expectThat(e.message).isEqualTo("Provider [$testProviderId] is not yet loaded!")
            }
        }

    @Test
    fun shouldGetAllApisAsPairs() =
        runTest(testDispatcher) {
            repository.addApiFromProvider(testProviderId, testProvider)

            val result = repository.getAll()
            expectThat(result).hasSize(1)
            expectThat(result.first().first).isEqualTo(testProviderId)
            expectThat(result.first().second).isA<TestProviderApi>()
        }

    @Test
    fun shouldGetAllApisAsValues() =
        runTest(testDispatcher) {
            repository.addApiFromProvider(testProviderId, testProvider)

            val result = repository.getApis()
            expectThat(result).hasSize(1)
            expectThat(result.first()).isA<TestProviderApi>()
        }

    @Test
    fun shouldReturnNullForNonExistentApi() =
        runTest(testDispatcher) {
            val result = repository.getApi("non-existent")
            expectThat(result).isNull()
        }

    @Test
    fun shouldRemoveApi() =
        runTest(testDispatcher) {
            repository.addApiFromProvider(testProviderId, testProvider)

            repository.removeApi(testProviderId)

            val result = repository.getApi(testProviderId)
            expectThat(result).isNull()
        }

    @Test
    fun shouldClearAllApis() =
        runTest(testDispatcher) {
            repository.addApiFromProvider(testProviderId, testProvider)
            repository.addApiFromProvider("another-provider", testProvider)

            repository.clearAll()

            expectThat(repository.getApis()).hasSize(0)
            expectThat(repository.getAll()).hasSize(0)
        }

    @Test
    fun shouldEmitRemoveOperationWhenApiRemoved() =
        runTest(testDispatcher) {
            repository.addApiFromProvider(testProviderId, testProvider)

            repository.observe().test {
                repository.removeApi(testProviderId)

                val emission = awaitItem()
                expectThat(emission).isA<CollectionsOperation.Map.Remove<String, ProviderApi>>()
                expectThat((emission as CollectionsOperation.Map.Remove).key).isEqualTo(testProviderId)
            }
        }

    @Test
    fun shouldHandleMultipleProviders() =
        runTest(testDispatcher) {
            val anotherTestProvider = TestProvider()
            val anotherProviderId = "another-provider"

            repository.addApiFromProvider(testProviderId, testProvider)
            repository.addApiFromProvider(anotherProviderId, anotherTestProvider)

            expectThat(repository.getApis()).hasSize(2)
            expectThat(repository.getApi(testProviderId)).isA<TestProviderApi>()
            expectThat(repository.getApi(anotherProviderId)).isA<TestProviderApi>()
        }
}
