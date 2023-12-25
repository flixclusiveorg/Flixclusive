package com.flixclusive.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

object CoroutinesUtils {
    suspend fun <T> CoroutineScope.asyncAwait(
        block: suspend CoroutineScope.() -> T
    )=  async { block() }.await()
}