package com.flixclusive.core.common.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

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

    /**
     * IO scope for coroutines that should run on the IO dispatcher.
     * This is typically used for tasks that involve I/O operations such as
     * network requests, file operations, or database access.
     */
    val ioScope: CoroutineScope

    /**
     * Default scope for coroutines for general-purpose tasks.
     * This is typically used for tasks that do not require specific threading
     * or isolation, such as simple computations or background tasks.
     */
    val defaultScope: CoroutineScope

    /**
     * Main scope for coroutines that should run on the main thread.
     * This is typically used for UI-related tasks or operations that need to
     * interact with the user interface.
     */
    val mainScope: CoroutineScope
}
