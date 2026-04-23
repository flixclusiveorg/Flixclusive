package com.flixclusive.core.common.domain

import androidx.compose.runtime.Stable
import com.flixclusive.core.common.locale.UiText


sealed class Async<out T> {
    data object Loading : Async<Nothing>()

    @Stable
    data class Success<T>(val data: T) : Async<T>()

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
    }

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
}
