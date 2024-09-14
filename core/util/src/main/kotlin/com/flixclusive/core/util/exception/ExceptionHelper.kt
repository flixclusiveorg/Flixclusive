package com.flixclusive.core.util.exception

import com.flixclusive.core.util.log.errorLog

/**
 * Executes the provided lambda [unsafeCall] safely, catching any exceptions and logging them.
 *
 * @param unsafeCall The lambda representing the possibly unsafe call.
 * @param message The optional message to log when the block fails.
 * @return The result of the lambda if it executes successfully, otherwise null.
 */
inline fun <T> safeCall(message: String? = null, unsafeCall: () -> T?): T? {
    return try {
        unsafeCall()
    } catch (e: Throwable) {
        errorLog(message ?: e.stackTraceToString())
        null
    }
}


val Throwable.actualMessage: String
    get() = localizedMessage ?: message ?: "UNKNOWN ERR"