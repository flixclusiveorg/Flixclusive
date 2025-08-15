package com.flixclusive.core.testing.dispatcher

import com.flixclusive.core.common.dispatchers.AppDispatchers
import kotlinx.coroutines.CoroutineDispatcher

object DispatcherTestDefaults {
    /**
     * Creates a test AppDispatchers instance that uses the provided test dispatcher.
     */
    fun createTestAppDispatchers(testDispatcher: CoroutineDispatcher): AppDispatchers {
        return object : AppDispatchers {
            override val default: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
            override val unconfined: CoroutineDispatcher = testDispatcher
        }
    }
}
