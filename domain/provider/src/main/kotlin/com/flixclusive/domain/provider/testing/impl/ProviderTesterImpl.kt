package com.flixclusive.domain.provider.testing.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.testing.ProviderTester
import com.flixclusive.domain.provider.testing.TestJobState
import com.flixclusive.domain.provider.testing.TestStage
import com.flixclusive.domain.provider.testing.impl.TestCases.ProviderTestCase
import com.flixclusive.domain.provider.testing.model.ProviderTestCaseResult
import com.flixclusive.domain.provider.testing.model.ProviderTestResult
import com.flixclusive.domain.provider.testing.model.TestStatus
import com.flixclusive.domain.provider.util.extensions.add
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.ProviderApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.koitharu.pausingcoroutinedispatcher.PausingJob
import org.koitharu.pausingcoroutinedispatcher.launchPausing
import org.koitharu.pausingcoroutinedispatcher.pausing
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

internal class ProviderTesterImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val providerApiRepository: ProviderApiRepository,
        private val providerRepository: ProviderRepository,
        private val appDispatchers: AppDispatchers,
        private val client: OkHttpClient,
    ) : ProviderTester {
        private val testCases by lazy { TestCases(appDispatchers) }

        private val _testStage = MutableStateFlow<TestStage>(TestStage.Idle(providerOnTest = null))
        override val testStage = _testStage.asStateFlow()

        private val _results = MutableStateFlow(emptyList<ProviderTestResult>())
        override val results = _results.asStateFlow()

        private var testJob: PausingJob? = null
        private val _testJobState = MutableStateFlow(TestJobState.IDLE)
        override val testJobState = _testJobState.asStateFlow()

        /**
         * The film backdrop/poster on test.
         * */
        private val _filmOnTest = MutableStateFlow<String?>(null)
        override val filmOnTest = _filmOnTest.asStateFlow()

        override fun start(providers: ArrayList<ProviderMetadata>) {
            testJob =
                appDispatchers.ioScope.launchPausing {
                    _testJobState.value = TestJobState.RUNNING

                    for (i in providers.indices) {
                        val provider = providers[i]

                        val testOutputs =
                            ProviderTestResult(
                                provider = provider.addTestCountSuffix(),
                            )

                        _results.add(testOutputs)
                        val apiTestCaseIndex =
                            testOutputs.add(
                                ProviderTestCaseResult(
                                    status = TestStatus.RUNNING,
                                    name = UiText.from(R.string.ptest_get_api),
                                ),
                            )

                        val api =
                            loadProviderApi(
                                metadata = provider,
                                updateOutput = {
                                    testOutputs.update(
                                        index = apiTestCaseIndex,
                                        output = it,
                                    )
                                },
                            ) ?: continue

                        updateFilmOnTest(api)

                        runTestCases(
                            api = api,
                            testCases = testCases.propertyTestCases,
                            stage = TestStage.Stage1(providerOnTest = provider),
                            addOutput = testOutputs::add,
                            updateOutput = testOutputs::update,
                        )

                        runTestCases(
                            api = api,
                            testCases = testCases.methodTestCases,
                            stage = TestStage.Stage2(providerOnTest = provider),
                            addOutput = testOutputs::add,
                            updateOutput = testOutputs::update,
                        )

                        _testStage.update { TestStage.Done(providerOnTest = provider) }
                    }

                    _testStage.update { TestStage.Idle(providerOnTest = null) }
                    _testJobState.value = TestJobState.IDLE
                }
        }

        override fun pause() {
            testJob?.pause()
            _testJobState.value = TestJobState.PAUSED
        }

        override fun resume() {
            testJob?.resume()
            _testJobState.value = TestJobState.RUNNING
        }

        override fun stop() {
            testJob?.cancel()
            testJob = null
            _testJobState.value = TestJobState.IDLE
            _testStage.update { TestStage.Idle(providerOnTest = null) }
        }

        private fun updateFilmOnTest(api: ProviderApi) {
            try {
                _filmOnTest.value = api.testFilm.backdropImage
                    ?: api.testFilm.posterImage
            } catch (_: Throwable) {
            }
        }

        private fun loadProviderApi(
            metadata: ProviderMetadata,
            updateOutput: (ProviderTestCaseResult) -> Unit,
        ): ProviderApi? {
            try {
                updateOutput(
                    ProviderTestCaseResult(
                        status = TestStatus.RUNNING,
                        name = UiText.from(R.string.ptest_get_api),
                    ),
                )

                val provider = providerRepository.getProvider(metadata.id)
                val api =
                    providerApiRepository.getApi(metadata.id)
                        ?: provider!!.getApi(
                            context = context,
                            client = client,
                        )

                updateOutput(
                    ProviderTestCaseResult(
                        status = TestStatus.SUCCESS,
                        name = UiText.from(R.string.ptest_get_api),
                        timeTaken = 0.milliseconds,
                        shortLog = UiText.from(R.string.ptest_success_get_api),
                        fullLog = UiText.from("${api.javaClass.simpleName} [HASHCODE:${api.hashCode()}]"),
                    ),
                )

                return api
            } catch (e: Throwable) {
                updateOutput(
                    ProviderTestCaseResult(
                        status = TestStatus.FAILURE,
                        name = UiText.from(R.string.ptest_get_api),
                        timeTaken = 0.milliseconds,
                        shortLog = UiText.from(R.string.ptest_error_get_api),
                        fullLog = UiText.from(e.stackTraceToString()),
                    ),
                )
                return null
            }
        }

        private suspend fun runTestCases(
            api: ProviderApi,
            testCases: List<ProviderTestCase>,
            stage: TestStage,
            addOutput: (ProviderTestCaseResult) -> Int,
            updateOutput: (Int, ProviderTestCaseResult) -> Unit,
        ) {
            _testStage.update { stage }
            for (i in testCases.indices) {
                val testCase = testCases[i]

                val addedIndex =
                    addOutput(
                        ProviderTestCaseResult(
                            status = TestStatus.RUNNING,
                            name = testCase.name,
                        ),
                    )

                // TODO: Check if this will be paused when `pause` is called.
                val finalOutput =
                    withContext(appDispatchers.io.pausing()) {
                        testCase.test(
                            // testName =
                            testCase.name,
                            // providerApi =
                            api,
                        )
                    }

                updateOutput(
                    // index =
                    addedIndex,
                    // output =
                    finalOutput,
                )

                val hasFailed =
                    testCase.stopTestOnFailure && finalOutput.status != TestStatus.SUCCESS

                if (hasFailed) {
                    break
                }
            }
        }

        private fun ProviderMetadata.addTestCountSuffix(): ProviderMetadata {
            val testCount =
                _results.value.count {
                    it.provider.id == id
                }

            if (testCount == 0) {
                return this
            }

            return copy(name = "$name ($testCount)")
        }
    }
