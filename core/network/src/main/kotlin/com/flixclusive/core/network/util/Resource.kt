package com.flixclusive.core.network.util

import androidx.annotation.StringRes
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.util.log.errorLog
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import com.flixclusive.core.locale.R as LocaleR

/**
 * A sealed class representing a resource that can be in one of three states: Success, Failure, or Loading.
 * @param T The type of data held by the resource.
 * @property data The data associated with the resource. It's nullable and defaults to null.
 * @property error The error associated with the resource. It's nullable and defaults to null.
 * @property isLoading Indicates whether the resource is in a loading state. It defaults to false.
 */
sealed class Resource<out T>(
    val data: T? = null,
    val error: UiText? = null,
    val isLoading: Boolean = false,
) {
    /**
     * Represents a successful state of the resource.
     * @param data The data associated with the resource.
     */
    class Success<T>(data: T) : Resource<T>(data = data, isLoading = false)

    /**
     * Represents a failure state of the resource.
     * @param error The error associated with the resource.
     */
    class Failure(error: UiText?) : Resource<Nothing>(error = error, isLoading = false) {
        constructor(@StringRes errorId: Int) : this(UiText.StringResource(errorId))
        constructor(error: Throwable?) : this(
            when (error) {
                null -> null
                else -> UiText.StringValue(error.stackTraceToString())
            }
        )
        constructor(error: String?) : this(
            when {
                error.isNullOrEmpty() -> null
                else -> UiText.StringValue(error)
            }
        )

        companion object {
            /**
             * Catches exceptions related to internet operations and converts them into a [Resource.Failure] object.
             *
             * @return A [Resource.Failure] object with an appropriate error message.
             */
            fun Throwable.toNetworkException(): Failure {
                return when (this) {
                    is SocketTimeoutException -> Failure(LocaleR.string.connection_timeout)
                    is ConnectException, is UnknownHostException -> Failure(
                        LocaleR.string.connection_failed)
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
                        Failure(errorMessage)
                    }
                    else -> {
                        errorLog(stackTraceToString())
                        Failure(this)
                    }
                }.also {
                    errorLog(stackTraceToString())
                }
            }
        }
    }

    /**
     * Represents a loading state of the resource.
     */
    data object Loading : Resource<Nothing>(isLoading = true)
}
