package com.flixclusive.domain.provider.test

import androidx.compose.runtime.mutableStateListOf
import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.domain.provider.test.ProviderTestCases.ProviderTestCase
import com.flixclusive.domain.provider.test.ProviderTestCases.methodTestCases
import com.flixclusive.domain.provider.test.ProviderTestCases.propertyTestCases
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.model.provider.id
import com.flixclusive.provider.ProviderApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koitharu.pausingcoroutinedispatcher.PausingJob
import org.koitharu.pausingcoroutinedispatcher.launchPausing
import org.koitharu.pausingcoroutinedispatcher.pausing
import javax.inject.Inject
import javax.inject.Singleton

private const val TEST_DELAY = 1000L

@Singleton
class TestProviderUseCase @Inject constructor(
    private val providerApiRepository: ProviderApiRepository,
    @ApplicationScope private val scope: CoroutineScope,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    private val _testStage = MutableStateFlow<TestStage>(TestStage.Idle(providerOnTest = null))
    val testStage = _testStage.asStateFlow()

    val results = mutableStateListOf<ProviderTestResult>()

    private var testJob: PausingJob? = null
    private val _testJobState = MutableStateFlow(TestJobState.IDLE)
    val testJobState = _testJobState.asStateFlow()

    operator fun invoke(
        providers: ArrayList<ProviderData>
    ) {
        testJob = scope.launchPausing {
            _testJobState.value = TestJobState.RUNNING

            for (i in providers.indices) {
                val provider = providers[i]

                val api = providerApiRepository.apiMap[provider.name]
                    ?: continue
                
                val testOutputs = ProviderTestResult(
                    provider = provider.addTestCountSuffix()
                )

                results.add(testOutputs)

                runTestCases(
                    api = api,
                    testCases = propertyTestCases,
                    stage = TestStage.Stage1(providerOnTest = provider),
                    addOutput = testOutputs::add,
                    updateOutput = testOutputs::update
                )

                runTestCases(
                    api = api,
                    testCases = methodTestCases,
                    stage = TestStage.Stage2(providerOnTest = provider),
                    addOutput = testOutputs::add,
                    updateOutput = testOutputs::update
                )

                _testStage.update { TestStage.Done(providerOnTest = provider) }
            }

            _testStage.update { TestStage.Idle(providerOnTest = null) }
            _testJobState.value = TestJobState.IDLE
        }
    }

    fun pause() {
        testJob?.pause()
        _testJobState.value = TestJobState.PAUSED
    }

    fun resume() {
        testJob?.resume()
        _testJobState.value = TestJobState.RUNNING
    }

    fun stop() {
        testJob?.cancel()
        testJob = null
        _testJobState.value = TestJobState.IDLE
    }

    private suspend fun runTestCases(
        api: ProviderApi,
        testCases: List<ProviderTestCase>,
        stage: TestStage,
        addOutput: (ProviderTestCaseOutput) -> Int,
        updateOutput: (Int, ProviderTestCaseOutput) -> Unit,
    ) {
        _testStage.update { stage }
        for (i in testCases.indices) {
            val testCase = testCases[i]

            val addedIndex = addOutput(
                ProviderTestCaseOutput(
                    status = TestStatus.RUNNING,
                    name = testCase.name
                )
            )

            val finalOutput = withContext(ioDispatcher.pausing()) {
                testCase.test(
                    /* testName = */ testCase.name,
                    /* providerApi = */ api,
                )
            }

            delay(TEST_DELAY / 2L)

            updateOutput(
                /* index = */ addedIndex,
                /* output = */ finalOutput
            )

            val hasFailed
                = testCase.stopTestOnFailure && finalOutput.status != TestStatus.SUCCESS

            if (hasFailed)
                break

            delay(TEST_DELAY)
        }
    }

    private fun ProviderData.addTestCountSuffix(): ProviderData {
        val testCount = results.count {
            it.provider.id == id
        }

        if (testCount == 0)
            return this

        return copy(name = "$name ($testCount)")
    }
}