package com.flixclusive.core.util.common.resource

import androidx.annotation.StringRes
import com.flixclusive.core.util.common.ui.UiText

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
                else -> UiText.StringValue(error.localizedMessage ?: error.message ?: error.stackTraceToString())
            }
        )
        constructor(error: String?) : this(
            when {
                error.isNullOrEmpty() -> null
                else -> UiText.StringValue(error)
            }
        )
    }

    /**
     * Represents a loading state of the resource.
     */
    data object Loading : Resource<Nothing>(isLoading = true)
}
