package com.flixclusive.core.common.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Interface for providing coroutine dispatchers throughout the application.
 *
 * This abstraction allows for easy testing by providing different dispatcher
 * implementations for production and test environments.
 */
interface AppDispatchers {
    /**
     * Dispatcher optimized for CPU-intensive work.
     */
    val default: CoroutineDispatcher

    /**
     * Dispatcher optimized for I/O operations such as reading from files,
     * network requests, or database operations.
     */
    val io: CoroutineDispatcher

    /**
     * Dispatcher for UI operations that should run on the main thread.
     */
    val main: CoroutineDispatcher

    /**
     * Dispatcher with unlimited parallelism for operations that should not
     * overwhelm system resources.
     */
    val unconfined: CoroutineDispatcher
}
