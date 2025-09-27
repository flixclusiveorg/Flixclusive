package com.flixclusive.domain.provider.testing.impl

import android.content.Context
import android.util.Log
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.testing.TestJobState
import com.flixclusive.domain.provider.testing.TestStage
import com.flixclusive.domain.provider.testing.model.TestStatus
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.filter.FilterList
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class ProviderTesterImplTest {
    private lateinit var providerTester: ProviderTesterImpl
    private lateinit var context: Context
    private lateinit var providerApiRepository: ProviderApiRepository
    private lateinit var providerRepository: ProviderRepository
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var client: OkHttpClient
    private lateinit var testApi: ProviderApi

    private val testDispatcher = StandardTestDispatcher()
    private val testProvider = mockk<Provider>(relaxed = true)
    private val testFilm = FilmTestDefaults.getMovie()
    private val testSearchFilms = List(10) {
        FilmTestDefaults.getFilmSearchItem(
            id = "id_$it",
            title = "Title $it",
        )
    }
    private val testCatalogs = listOf(
        ProviderCatalog(
            name = "Test Catalog",
            url = "test-url",
            canPaginate = true,
            providerId = "test-provider",
        ),
    )
    private val testFilters = FilterList()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } answers {
            println(args[1])
            0
        }

        context = mockk(relaxed = true)
        providerApiRepository = mockk(relaxed = true)
        providerRepository = mockk(relaxed = true)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
        client = mockk(relaxed = true)
        testApi = mockk(relaxed = true) {
            every { testFilm } returns this@ProviderTesterImplTest.testFilm
            every { catalogs } returns testCatalogs
            every { filters } returns testFilters
            coEvery { getMetadata(any()) } returns this@ProviderTesterImplTest.testFilm
            coEvery {
                search(any(), any(), any(), any(), any(), any())
            } returns SearchResponseData(results = testSearchFilms)
            coEvery { getCatalogItems(any(), any()) } returns SearchResponseData(results = testSearchFilms)
            coEvery { getLinks(any(), any<FilmMetadata>(), any(), any()) } returns Unit
        }

        providerTester = ProviderTesterImpl(
            context = context,
            providerApiRepository = providerApiRepository,
            providerRepository = providerRepository,
            appDispatchers = appDispatchers,
            client = client,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsCorrect() {
        expectThat(providerTester.testStage.value).isA<TestStage.Idle>()
        expectThat(providerTester.testJobState.value).isEqualTo(TestJobState.IDLE)
        expectThat(providerTester.results.value).hasSize(0)
        expectThat(providerTester.filmOnTest.value).isNull()
    }

    @Test
    fun clearResetsResults() {
        // Given some results exist
        val testMetadata = ProviderTestDefaults.getProviderMetadata()
        val providers = arrayListOf(testMetadata)

        runTest(testDispatcher) {
            // Mock successful provider loading
            every { providerRepository.getProvider(testMetadata.id) } returns testProvider
            every { providerApiRepository.getApi(testMetadata.id) } returns testApi
            every { testProvider.getApi(context, client) } returns testApi

            providerTester.start(providers)
            advanceUntilIdle()

            // When clear is called
            providerTester.clear()

            // Then results are empty
            expectThat(providerTester.results.value).hasSize(0)
        }
    }

    @Test
    fun pauseChangesTestJobStateToPaused() =
        runTest(testDispatcher) {
            val testMetadata = ProviderTestDefaults.getProviderMetadata()
            val providers = arrayListOf(testMetadata)

            // Mock successful provider loading
            every { providerRepository.getProvider(testMetadata.id) } returns testProvider
            every { providerApiRepository.getApi(testMetadata.id) } returns testApi
            every { testProvider.getApi(context, client) } returns testApi

            providerTester.start(providers)

            // When pause is called
            providerTester.pause()

            // Then job state changes to paused
            expectThat(providerTester.testJobState.value).isEqualTo(TestJobState.PAUSED)
        }

    @Test
    fun resumeChangesTestJobStateToRunning() =
        runTest(testDispatcher) {
            val testMetadata = ProviderTestDefaults.getProviderMetadata()
            val providers = arrayListOf(testMetadata)

            // Mock successful provider loading
            every { providerRepository.getProvider(testMetadata.id) } returns testProvider
            every { providerApiRepository.getApi(testMetadata.id) } returns testApi
            every { testProvider.getApi(context, client) } returns testApi

            providerTester.start(providers)
            providerTester.pause()

            // When resume is called
            providerTester.resume()

            // Then job state changes back to running
            expectThat(providerTester.testJobState.value).isEqualTo(TestJobState.RUNNING)
        }

    @Test
    fun stopCancelsJobAndResetsState() =
        runTest(testDispatcher) {
            val testMetadata = ProviderTestDefaults.getProviderMetadata()
            val providers = arrayListOf(testMetadata)

            // Mock successful provider loading
            every { providerRepository.getProvider(testMetadata.id) } returns testProvider
            every { providerApiRepository.getApi(testMetadata.id) } returns testApi
            every { testProvider.getApi(context, client) } returns testApi

            providerTester.start(providers)

            // When stop is called
            providerTester.stop()

            // Then job state is idle and test stage is reset
            expectThat(providerTester.testJobState.value).isEqualTo(TestJobState.IDLE)
            expectThat(providerTester.testStage.value).isA<TestStage.Idle>()
        }

    @Test
    fun startWithSuccessfulProviderLoading() =
        runTest(testDispatcher) {
            val testMetadata = ProviderTestDefaults.getProviderMetadata()
            val providers = arrayListOf(testMetadata)

            // Mock successful provider loading
            every { providerRepository.getProvider(testMetadata.id) } returns testProvider
            every { providerApiRepository.getApi(testMetadata.id) } returns testApi
            every { testProvider.getApi(context, client) } returns testApi

            providerTester.start(providers)
            advanceUntilIdle()

            turbineScope {
                val testJobStateReceiver = providerTester.testJobState.testIn(this)
                val resultsReceiver = providerTester.results.testIn(this)

                // Verify final state after execution
                expectThat(testJobStateReceiver.awaitItem()).isEqualTo(TestJobState.IDLE)

                val results = resultsReceiver.awaitItem()
                expectThat(results).hasSize(1)

                val testCases = results.first().outputs.testIn(this)
                expectThat(testCases.awaitItem()).hasSize(8)

                testJobStateReceiver.cancelAndIgnoreRemainingEvents()
                resultsReceiver.cancelAndIgnoreRemainingEvents()
                testCases.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun startWithFailedProviderLoadingFromRepository() =
        runTest(testDispatcher) {
            val testMetadata = ProviderTestDefaults.getProviderMetadata()
            val providers = arrayListOf(testMetadata)

            // Mock failed provider loading - repository returns null
            every { providerRepository.getProvider(testMetadata.id) } returns null
            every { providerApiRepository.getApi(testMetadata.id) } returns null

            turbineScope {
                val resultsReceiver = providerTester.results.testIn(this)
                val testStageReceiver = providerTester.testStage.testIn(this)

                providerTester.start(providers)
                advanceUntilIdle()

                expectThat(resultsReceiver.awaitItem()).hasSize(0) // Initial empty state
                expectThat(testStageReceiver.awaitItem()).isA<TestStage.Idle>() // Initial idle state

                val results = resultsReceiver.awaitItem()
                expectThat(results).hasSize(1)

                // Verify that the test result contains a failure case
                val testResult = results.first()
                expectThat(testResult.provider.id).isEqualTo(testMetadata.id)

                // Test the outputs StateFlow using testIn
                val outputsReceiver = testResult.outputs.testIn(this)
                val outputs = outputsReceiver.awaitItem()
                expectThat(outputs).hasSize(1)
                expectThat(outputs.first().status).isEqualTo(TestStatus.FAILURE)

                // Clean up turbine receivers
                resultsReceiver.cancelAndIgnoreRemainingEvents()
                testStageReceiver.cancelAndIgnoreRemainingEvents()
                outputsReceiver.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun startWithExceptionDuringProviderLoading() =
        runTest(testDispatcher) {
            val testMetadata = ProviderTestDefaults.getProviderMetadata()
            val providers = arrayListOf(testMetadata)
            val testException = RuntimeException("Test exception")

            // Mock exception during provider loading
            every { providerRepository.getProvider(testMetadata.id) } throws testException
            every { providerApiRepository.getApi(testMetadata.id) } returns null

            turbineScope {
                val resultsReceiver = providerTester.results.testIn(this)
                val testStageReceiver = providerTester.testStage.testIn(this)

                providerTester.start(providers)
                advanceUntilIdle()

                expectThat(resultsReceiver.awaitItem()).hasSize(0) // Initial empty state
                expectThat(testStageReceiver.awaitItem()).isA<TestStage.Idle>() // Initial idle state

                val results = resultsReceiver.awaitItem()
                expectThat(results).hasSize(1)

                // Verify that the test result contains a failure case with exception details
                val testResult = results.first()

                // Test the outputs StateFlow using testIn
                val outputsReceiver = testResult.outputs.testIn(this)
                val outputs = outputsReceiver.awaitItem()
                expectThat(outputs).hasSize(1)
                expectThat(outputs.first().status).isEqualTo(TestStatus.FAILURE)

                // Clean up turbine receivers
                resultsReceiver.cancelAndIgnoreRemainingEvents()
                testStageReceiver.cancelAndIgnoreRemainingEvents()
                outputsReceiver.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun filmOnTestIsUpdatedWhenApiIsLoaded() =
        runTest(testDispatcher) {
            val testMetadata = ProviderTestDefaults.getProviderMetadata()
            val providers = arrayListOf(testMetadata)
            val testBackdropUrl = "https://example.com/backdrop.jpg"

            // Mock successful provider loading with backdrop image
            every { providerRepository.getProvider(testMetadata.id) } returns testProvider
            every { providerApiRepository.getApi(testMetadata.id) } returns testApi
            every { testProvider.getApi(context, client) } returns testApi
            every { testApi.testFilm.backdropImage } returns testBackdropUrl

            providerTester.filmOnTest.test {
                providerTester.start(providers)
                advanceUntilIdle()

                expectThat(awaitItem()).isNull() // Initial null state
                expectThat(awaitItem()).isEqualTo(testBackdropUrl) // Updated with backdrop
            }
        }

    @Test
    fun filmOnTestFallbacksToPosterWhenBackdropIsNull() =
        runTest(testDispatcher) {
            val testMetadata = ProviderTestDefaults.getProviderMetadata()
            val providers = arrayListOf(testMetadata)
            val testPosterUrl = "https://example.com/poster.jpg"

            // Mock successful provider loading with poster image (no backdrop)
            every { providerRepository.getProvider(testMetadata.id) } returns testProvider
            every { providerApiRepository.getApi(testMetadata.id) } returns testApi
            every { testProvider.getApi(context, client) } returns testApi
            every { testApi.testFilm.backdropImage } returns null
            every { testApi.testFilm.posterImage } returns testPosterUrl

            providerTester.filmOnTest.test {
                providerTester.start(providers)
                advanceUntilIdle()

                expectThat(awaitItem()).isNull() // Initial null state
                expectThat(awaitItem()).isEqualTo(testPosterUrl) // Updated with poster fallback
            }
        }

    @Test
    fun startWithMultipleProviders() =
        runTest(testDispatcher) {
            val testMetadata1 = ProviderTestDefaults.getProviderMetadata(id = "provider1")
            val testMetadata2 = ProviderTestDefaults.getProviderMetadata(id = "provider2")
            val providers = arrayListOf(testMetadata1, testMetadata2)

            val testProvider2 = mockk<Provider>(relaxed = true)
            val testApi2 = mockk<ProviderApi>(relaxed = true)

            // Mock successful loading for both providers
            every { providerRepository.getProvider(testMetadata1.id) } returns testProvider
            every { providerApiRepository.getApi(testMetadata1.id) } returns testApi
            every { testProvider.getApi(context, client) } returns testApi

            every { providerRepository.getProvider(testMetadata2.id) } returns testProvider2
            every { providerApiRepository.getApi(testMetadata2.id) } returns testApi2
            every { testProvider2.getApi(context, client) } returns testApi2
            every { testApi2.testFilm } returns testFilm

            providerTester.start(providers)
            advanceUntilIdle()

            turbineScope {
                val testJobState = providerTester.testJobState.testIn(this)
                val results = providerTester.results.testIn(this)

                expectThat(testJobState.awaitItem()).isEqualTo(TestJobState.IDLE)
                expectThat(results.awaitItem()) {
                    hasSize(2)
                    get { map { it.provider.id } }.isEqualTo(listOf("provider1", "provider2"))
                }

                testJobState.cancelAndIgnoreRemainingEvents()
                results.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun repositoryMethodsAreCalledInCorrectOrder() =
        runTest(testDispatcher) {
            val testMetadata = ProviderTestDefaults.getProviderMetadata()
            val providers = arrayListOf(testMetadata)

            // Mock successful provider loading
            every { providerRepository.getProvider(testMetadata.id) } returns testProvider
            every { providerApiRepository.getApi(testMetadata.id) } returns null
            every { testProvider.getApi(context, client) } returns testApi

            providerTester.start(providers)
            advanceUntilIdle()

            // Verify the correct sequence of repository calls
            verify { providerRepository.getProvider(testMetadata.id) }
            verify { providerApiRepository.getApi(testMetadata.id) }
            verify { testProvider.getApi(context, client) }
        }
}
