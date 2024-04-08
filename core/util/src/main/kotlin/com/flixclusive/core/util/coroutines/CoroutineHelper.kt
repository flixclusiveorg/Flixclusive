package com.flixclusive.core.util.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Asynchronously maps the elements of the iterable using the specified suspending function [mapper].
 * @param mapper The suspending function to apply to each element.
 * @return A list containing the results of applying the [mapper] function to each element.
 */
suspend fun <T, R> Iterable<T>.mapAsync(
    mapper: suspend (T) -> R
): List<R> = coroutineScope { map { async { mapper(it) } }.awaitAll() }

suspend fun <T, R> Array<out T>.mapAsync(
    mapper: suspend (T) -> R
): List<R> = coroutineScope { map { async { mapper(it) } }.awaitAll() }

/**
 * Asynchronously maps the elements of the iterable with their index using the specified suspending function [mapper].
 * @param mapper The suspending function to apply to each indexed element.
 * @return A list containing the results of applying the [mapper] function to each indexed element.
 */
suspend fun <T, R> Iterable<T>.mapIndexedAsync(
    mapper: suspend (Int, T) -> R
): List<R> = coroutineScope { mapIndexed { i, item -> async { mapper(i, item) } }.awaitAll() }

/**
 * Executes multiple suspend functions asynchronously and waits for all to complete.
 * @param transforms The suspend functions to execute asynchronously.
 * @return A list containing the results of all the suspend functions.
 */
fun <R> asyncCalls(
    vararg transforms: suspend () -> R,
) = runBlocking {
    transforms.map {
        async { it.invoke() }
    }.awaitAll()
}

private val ioCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


/**
 * Launches a coroutine in the IO coroutine scope.
 * @param block The suspending lambda to execute in the IO coroutine scope.
 * @return A [Job] object representing the coroutine job.
 */
fun ioLaunch(block: suspend () -> Unit): Job {
    return ioCoroutineScope.launch {
        block()
    }
}

/**
 * Calls the specified suspending function in the IO dispatcher.
 * @param block The suspending lambda to execute.
 * @return The result of the suspending function.
 */
suspend fun <T> ioCall(block: suspend () -> T): T {
    return withContext(Dispatchers.IO) {
        block()
    }
}
