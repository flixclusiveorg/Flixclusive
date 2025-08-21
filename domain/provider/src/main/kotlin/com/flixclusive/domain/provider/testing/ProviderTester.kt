package com.flixclusive.domain.provider.testing

import com.flixclusive.domain.provider.testing.model.ProviderTestResult
import com.flixclusive.model.provider.ProviderMetadata
import kotlinx.coroutines.flow.StateFlow

enum class TestJobState {
    PAUSED,
    RUNNING,
    IDLE,
}

/**
 * Main service for testing providers.
 *
 * This service is responsible for running tests on providers, managing the test stages,
 * and providing results of the tests.
 * */
interface ProviderTester {
    /**
     * The current stage of the test and the provider that is currently being tested.
     * */
    val testStage: StateFlow<TestStage>

    /**
     * The results of the tests.
     * */
    val results: StateFlow<List<ProviderTestResult>>

    /**
     * The current state of the test job. It can be one of the following:
     * - [TestJobState.IDLE]: No test is running.
     * - [TestJobState.RUNNING]: A test is currently running.
     * - [TestJobState.PAUSED]: A test is paused.
     * */
    val testJobState: StateFlow<TestJobState>

    /**
     * The film backdrop/poster on test.
     * This is used to display the film that is currently being tested.
     *
     * It can be null if no film is currently being tested.
     * If a film is being tested, it will contain the URL of the film's backdrop or poster image.
     * */
    val filmOnTest: StateFlow<String?>

    /**
     * Starts the testing process for the given providers.
     *
     * @param providers The list of providers to test.
     * */
    fun start(providers: ArrayList<ProviderMetadata>)

    /**
     * Pauses the current test job if it is running.
     * */
    fun pause()

    /**
     * Resumes the paused test job if it was previously paused.
     * */
    fun resume()

    /**
     * Stops the current test job if it is running or paused.
     * */
    fun stop()
}
