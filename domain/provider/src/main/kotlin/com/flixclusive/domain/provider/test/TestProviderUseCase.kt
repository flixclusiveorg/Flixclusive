package com.flixclusive.domain.provider.test

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.domain.provider.test.ProviderTestCases.ProviderTestCase
import com.flixclusive.domain.provider.test.ProviderTestCases.methodTestCases
import com.flixclusive.domain.provider.test.ProviderTestCases.propertyTestCases
import com.flixclusive.domain.provider.util.StringHelper.createString
import com.flixclusive.domain.provider.util.StringHelper.getString
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.provider.ProviderApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.koitharu.pausingcoroutinedispatcher.PausingJob
import org.koitharu.pausingcoroutinedispatcher.launchPausing
import org.koitharu.pausingcoroutinedispatcher.pausing
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import com.flixclusive.core.locale.R as LocaleR

private const val TEST_DELAY = 1000L

@Singleton
class TestProviderUseCase @Inject constructor(
    private val providerApiRepository: ProviderApiRepository,
    private val providerManager: ProviderManager,
    private val client: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    private val _testStage = MutableStateFlow<TestStage>(TestStage.Idle(providerOnTest = null))
    val testStage = _testStage.asStateFlow()

    val results = mutableStateListOf<ProviderTestResult>()

    private var testJob: PausingJob? = null
    private val _testJobState = MutableStateFlow(TestJobState.IDLE)
    val testJobState = _testJobState.asStateFlow()

    /**
     * The film backdrop/poster on test.
     * */
    private val _filmOnTest = MutableStateFlow<String?>(null)
    val filmOnTest = _filmOnTest.asStateFlow()

    operator fun invoke(
        providers: ArrayList<ProviderData>
    ) {
        testJob = AppDispatchers.IO.scope.launchPausing {
            _testJobState.value = TestJobState.RUNNING

            for (i in providers.indices) {
                val provider = providers[i]

                val testOutputs = ProviderTestResult(
                    provider = provider.addTestCountSuffix()
                )

                results.add(testOutputs)
                val apiTestCaseIndex = testOutputs.add(
                    ProviderTestCaseOutput(
                        status = TestStatus.RUNNING,
                        name = getString(LocaleR.string.ptest_get_api)
                    )
                )

                val api = loadProviderApi(
                    providerData = provider,
                    updateOutput = {
                        testOutputs.update(
                            index = apiTestCaseIndex,
                            output = it
                        )
                    }
                ) ?: continue

                updateFilmOnTest(api)

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
        _testStage.update { TestStage.Idle(providerOnTest = null) }
    }

    private fun updateFilmOnTest(api: ProviderApi) {
        try {
            _filmOnTest.value = api.testFilm.backdropImage
                ?: api.testFilm.posterImage
        } catch (_: Throwable) {}
    }

    private fun loadProviderApi(
        providerData: ProviderData,
        updateOutput: (ProviderTestCaseOutput) -> Unit,
    ): ProviderApi? {
        try {
            updateOutput(
                ProviderTestCaseOutput(
                    status = TestStatus.RUNNING,
                    name = getString(LocaleR.string.ptest_get_api)
                )
            )

            val provider = providerManager.providers[providerData.name]
            val api = providerApiRepository.apiMap[providerData.name]
                ?: provider!!.getApi(
                    context = context,
                    client = client
                )

            updateOutput(
                ProviderTestCaseOutput(
                    status = TestStatus.SUCCESS,
                    name = getString(LocaleR.string.ptest_get_api),
                    timeTaken = 0.milliseconds,
                    shortLog = getString(LocaleR.string.ptest_success_get_api),
                    fullLog = createString("${api.javaClass.simpleName} [HASHCODE:${api.hashCode()}]")
                )
            )

            return api
        } catch (e: Throwable) {
            updateOutput(
                ProviderTestCaseOutput(
                    status = TestStatus.FAILURE,
                    name = getString(LocaleR.string.ptest_get_api),
                    timeTaken = 0.milliseconds,
                    shortLog = getString(LocaleR.string.ptest_error_get_api),
                    fullLog = createString(e.stackTraceToString())
                )
            )
            return null
        }
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

            val finalOutput = withContext(AppDispatchers.IO.dispatcher.pausing()) {
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
            it.provider.id.removeCountSuffix() == id
        }

        if (testCount == 0)
            return this

        return copy(name = "$name ($testCount)")
    }

    private fun String.removeCountSuffix(): String {
        val regex = """\s*\(\d+\)""".toRegex() // Matches " (number)" at the end of the string
        return replace(regex, "")
    }
}