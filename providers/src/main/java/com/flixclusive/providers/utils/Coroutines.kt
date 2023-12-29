package com.flixclusive.providers.utils

import com.flixclusive.providers.R
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

/**
 *
 * Fast list iteration:
 * https://www.linkedin.com/pulse/faster-list-iteration-using-kotlin-coroutines-elyes-mansour
 *
 * */
suspend fun <T, R> Iterable<T>.mapAsync(
    mapper: suspend (T) -> R
): List<R> = coroutineScope { map { async { mapper(it) } }.awaitAll() }

/**
 *
 * Fast list iteration with index
 * */
suspend fun <T, R> Iterable<T>.mapIndexedAsync(
    mapper: suspend (Int, T) -> R
): List<R> = coroutineScope { mapIndexed { i, item -> async { mapper(i, item) } }.awaitAll() }


/**
 *
 * From Cloudstream `argamap`, aims to call
 * multiple suspend functions asynchronously
 *
 * https://github.com/recloudstream/cloudstream/blob/9d3b2ba3d2497e3a494f87b88b41687df7f5223a/app/src/main/java/com/lagradost/cloudstream3/ParCollections.kt#L76
 *
 * */
fun <R> asyncCalls(
    vararg transforms: suspend () -> R,
) = runBlocking {
    transforms.map {
        async { it.invoke() }
    }.map { it.await() }
}