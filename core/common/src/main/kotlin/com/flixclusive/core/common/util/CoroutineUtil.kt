package com.flixclusive.core.common.util

import com.flixclusive.core.util.log.errorLog
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Retries a suspending [block] with exponential backoff.
 *
 * @param maxRetries The maximum number of attempts.
 * @param initialDelayMs The initial delay before the first retry in milliseconds.
 * @param maxDelayMs The maximum delay between retries in milliseconds.
 * @param factor The multiplier for the delay on each retry.
 * @param block The suspending block to execute.
 */
@Throws
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000L,
    maxDelayMs: Long = 5000L,
    factor: Double = 2.0,
    onError: (Throwable) -> Unit = {},
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    repeat(maxRetries - 1) {
        try {
            return block()
        } catch (e: Throwable) {
            errorLog("Retrying with backoff [$it]: ${e.stackTraceToString()}")
            onError(e)
        }
        delay(currentDelay.milliseconds)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
    }

    return block() // Final attempt
}
