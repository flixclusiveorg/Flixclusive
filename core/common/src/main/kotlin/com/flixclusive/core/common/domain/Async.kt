package com.flixclusive.core.common.domain

import androidx.compose.runtime.Stable
import com.flixclusive.core.common.locale.UiText


sealed class Async<out T> {
    data object Loading : Async<Nothing>()

    @Stable
    data class Success<T>(val data: T) : Async<T>() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success<*>) return false
            return data == other.data
        }

        override fun hashCode(): Int {
            return data?.hashCode() ?: 0
        }
    }

    @Stable
    data class Failure(
        val message: UiText,
        val cause: Throwable? = null,
    ) : Async<Nothing>() {
        constructor(cause: Throwable) : this(
            message = UiText.from(cause.message ?: "An unknown error occurred"),
            cause = cause,
        )

        constructor(message: String) : this(
            message = UiText.from(message),
            cause = null,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Failure) return false
            return message == other.message && cause?.message == other.cause?.message
        }

        override fun hashCode(): Int {
            var result = message.hashCode()
            result = 31 * result + (cause?.hashCode() ?: 0)
            return result
        }
    }

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
}
