package com.flixclusive.domain.common

import androidx.annotation.StringRes
import com.flixclusive.common.UiText


sealed class Resource<out T>(
    val data: T? = null,
    val error: UiText? = null,
    val isLoading: Boolean = false,
) {
    class Success<T>(data: T) : Resource<T>(data = data, isLoading = false)
    class Failure(error: UiText?) : Resource<Nothing>(error = error, isLoading = false) {
        constructor(@StringRes errorId: Int) : this(UiText.StringResource(errorId))
        constructor(error: String?) : this(
            if(error.isNullOrEmpty()) null else UiText.StringValue(error)
        )
    }

    data object Loading : Resource<Nothing>(isLoading = true)
}


