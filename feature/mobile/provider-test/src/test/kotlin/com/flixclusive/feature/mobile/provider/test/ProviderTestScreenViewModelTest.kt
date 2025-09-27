package com.flixclusive.feature.mobile.provider.test

import app.cash.turbine.test
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.testing.ProviderTester
import com.flixclusive.domain.provider.testing.TestJobState
import com.flixclusive.domain.provider.testing.TestStage
import com.flixclusive.domain.provider.testing.model.ProviderTestResult
import com.flixclusive.model.provider.ProviderMetadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ProviderTestScreenViewModelTest {
    private lateinit var providerTester: ProviderTester
    private lateinit var providerRepository: ProviderRepository
    private lateinit var viewModel: ProviderTestScreenViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val resultsFlow = MutableStateFlow<List<ProviderTestResult>>(emptyList())
    private val testStageFlow = MutableStateFlow<TestStage>(TestStage.Idle)
    private val testJobStateFlow = MutableStateFlow(TestJobState.IDLE)
    private val filmOnTestFlow = MutableStateFlow<String?>(null)

    private val dummyProvider1 = DummyDataForPreview.getDummyProviderMetadata(
        id = "provider1",
        name = "Provider 1",
    )
    private val dummyProvider2 = DummyDataForPreview.getDummyProviderMetadata(
        id = "provider2",
        name = "Provider 2",
    )
    private val dummyProvider3 = DummyDataForPreview.getDummyProviderMetadata(
        id = "provider3",
        name = "Provider 3",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        providerTester = mockk(relaxed = true)
        providerRepository = mockk(relaxed = true)

        every { providerTester.results } returns resultsFlow.asStateFlow()
        every { providerTester.testStage } returns testStageFlow.asStateFlow()
        every { providerTester.testJobState } returns testJobStateFlow.asStateFlow()
        every { providerTester.filmOnTest } returns filmOnTestFlow.asStateFlow()

        viewModel = ProviderTestScreenViewModel(
            providerTester = providerTester,
            providerRepository = providerRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `property delegations should return correct flows`() =
        runTest(testDispatcher) {
            viewModel.results.test {
                expectThat(awaitItem()).isEqualTo(emptyList<ProviderTestResult>())
            }

            viewModel.testStage.test {
                expectThat(awaitItem()).isEqualTo(TestStage.Idle)
            }

            viewModel.testJobState.test {
                expectThat(awaitItem()).isEqualTo(TestJobState.IDLE)
            }

            viewModel.filmOnTest.test {
                expectThat(awaitItem()).isEqualTo(null)
            }
        }

    @Test
    fun `stopTests should call providerTester stop`() {
        viewModel.stopTests()

        verify { providerTester.stop() }
    }

    @Test
    fun `pauseTests should call providerTester pause`() {
        viewModel.pauseTests()

        verify { providerTester.pause() }
    }

    @Test
    fun `resumeTests should call providerTester resume`() {
        viewModel.resumeTests()

        verify { providerTester.resume() }
    }

    @Test
    fun `clearTests should call providerTester clear`() {
        viewModel.clearTests()

        verify { providerTester.clear() }
    }

    @Test
    fun `startTests with empty providers list should use all providers from repository`() {
        val allProviders = listOf(dummyProvider1, dummyProvider2, dummyProvider3)
        every { providerRepository.getProviders() } returns allProviders

        val result = viewModel.startTests(arrayListOf())

        expectThat(result).isEqualTo(StartTestResult.STARTED)
        verify { providerTester.start(ArrayList(allProviders)) }
    }

    @Test
    fun `startTests with specific providers should use provided providers`() {
        val providers = arrayListOf(dummyProvider1, dummyProvider2)

        val result = viewModel.startTests(providers)

        expectThat(result).isEqualTo(StartTestResult.STARTED)
        verify { providerTester.start(providers) }
    }

    @Test
    fun `startTests should return SHOW_WARNING when providers already tested and flags are false`() {
        val providers = arrayListOf(dummyProvider1)
        val testResult = ProviderTestResult(provider = dummyProvider1)
        resultsFlow.value = listOf(testResult)

        val result = viewModel.startTests(
            providers = providers,
            skipTestedProviders = false,
            testAgainIfTested = false,
        )

        expectThat(result).isEqualTo(StartTestResult.SHOW_WARNING)
        verify(exactly = 0) { providerTester.start(any()) }
    }

    @Test
    fun `startTests should start tests when testAgainIfTested is true`() {
        val providers = arrayListOf(dummyProvider1)
        val testResult = ProviderTestResult(provider = dummyProvider1)
        resultsFlow.value = listOf(testResult)

        val result = viewModel.startTests(
            providers = providers,
            testAgainIfTested = true,
        )

        expectThat(result).isEqualTo(StartTestResult.STARTED)
        verify { providerTester.start(providers) }
    }

    @Test
    fun `startTests should filter tested providers when skipTestedProviders is true`() {
        val providers = arrayListOf(dummyProvider1, dummyProvider2, dummyProvider3)
        val testResult = ProviderTestResult(provider = dummyProvider1)
        resultsFlow.value = listOf(testResult)

        val result = viewModel.startTests(
            providers = providers,
            skipTestedProviders = true,
        )

        expectThat(result).isEqualTo(StartTestResult.STARTED)
        verify {
            providerTester.start(
                match<ArrayList<ProviderMetadata>> { providersToTest ->
                    providersToTest.size == 2 &&
                        providersToTest.contains(dummyProvider2) &&
                        providersToTest.contains(dummyProvider3) &&
                        !providersToTest.contains(dummyProvider1)
                },
            )
        }
    }

    @Test
    fun `startTests with empty list after filtering should still start with empty list`() {
        val providers = arrayListOf(dummyProvider1)
        val testResult = ProviderTestResult(provider = dummyProvider1)
        resultsFlow.value = listOf(testResult)

        val result = viewModel.startTests(
            providers = providers,
            skipTestedProviders = true,
        )

        expectThat(result).isEqualTo(StartTestResult.STARTED)
        verify {
            providerTester.start(
                match<ArrayList<ProviderMetadata>> { it.isEmpty() },
            )
        }
    }

    @Test
    fun `startTests should handle mixed tested and untested providers correctly`() {
        val providers = arrayListOf(dummyProvider1, dummyProvider2)
        val testResult1 = ProviderTestResult(provider = dummyProvider1)
        val testResult3 = ProviderTestResult(provider = dummyProvider3) // Different provider not in input
        resultsFlow.value = listOf(testResult1, testResult3)

        val result = viewModel.startTests(
            providers = providers,
            skipTestedProviders = true,
        )

        expectThat(result).isEqualTo(StartTestResult.STARTED)
        verify {
            providerTester.start(
                match<ArrayList<ProviderMetadata>> { providersToTest ->
                    providersToTest.size == 1 &&
                        providersToTest.contains(dummyProvider2)
                },
            )
        }
    }
}
