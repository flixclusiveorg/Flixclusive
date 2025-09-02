package com.flixclusive.core.common.pagination

import androidx.annotation.StringRes
import com.flixclusive.core.common.R
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.pagination.PagingDataState.Error
import com.flixclusive.core.common.pagination.PagingDataState.Loading
import com.flixclusive.core.common.pagination.PagingDataState.Success

/**
 * Represents the state of paginated data loading.
 *
 * This is a sealed class with three possible states:
 * - [Loading]: Indicates that data is currently being loaded.
 * - [Success]: Indicates that data has been successfully loaded, with a flag to indicate if all data has been loaded ([Success.isExhausted]).
 * - [Error]: Indicates that an error occurred during data loading, with an associated error message ([UiText]).
 *
 * The class also provides convenience properties to check if the state is loading or done.
 */
sealed class PagingDataState {
    data object Loading : PagingDataState()

    data class Success(
        val isExhausted: Boolean,
    ) : PagingDataState()

    data class Error(
        val error: UiText,
    ) : PagingDataState() {
        constructor(@StringRes errorId: Int) : this(UiText.from(errorId))
        constructor(error: String) : this(UiText.from(error))
        constructor(exception: Throwable) :
            this(
                exception.localizedMessage?.let { UiText.from(it) }
                    ?: UiText.from(R.string.paging_state_default_error),
            )
    }

    val isLoading get() = this is Loading
    val isDone get() = this is Success && isExhausted
    val isError get() = this is Error
}
