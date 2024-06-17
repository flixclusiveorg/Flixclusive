package com.flixclusive.core.util.exception

import com.flixclusive.core.util.R
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.log.errorLog
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException


/**
 * Catches exceptions related to internet operations and converts them into a [Resource.Failure] object.
 * @return A [Resource.Failure] object with an appropriate error message.
 */
fun Exception.catchInternetRelatedException(): Resource.Failure {
    val defaultMessage = localizedMessage ?: message ?: "Unknown error occurred"

    return when (this) {
        is SocketTimeoutException -> Resource.Failure(R.string.connection_timeout)
        is ConnectException, is UnknownHostException -> Resource.Failure(R.string.connection_failed)
        is HttpException -> {
            val errorMessage = "HTTP Code: ${code()}"
            errorLog(errorMessage)
            errorLog("Headers: ${this.response()?.headers()}")
            errorLog("Body: ${this.response()?.errorBody()?.string()}")
            Resource.Failure(errorMessage)
        }
        is SSLException -> {
            val errorMessage = "SSL error: $localizedMessage; Check if your system date is correct."
            errorLog(errorMessage)
            Resource.Failure(errorMessage)
        }
        else -> {
            errorLog(defaultMessage)
            Resource.Failure(defaultMessage)
        }
    }.also {
        errorLog(stackTraceToString())
    }
}

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
    } catch (e: Exception) {
        errorLog(message ?: e.stackTraceToString())
        null
    }
}
