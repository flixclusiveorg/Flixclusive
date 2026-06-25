package com.flixclusive.core.common.domain

import androidx.annotation.StringRes
import com.flixclusive.core.common.R
import com.flixclusive.core.common.locale.UiText

/**
 * Represents the state of paginated data loading.
 *
 * This is a sealed class with three possible states:
 * - [Loading]: Indicates that data is currently being loaded.
 * - [Exhausted]: Indicates that data has been successfully loaded, with a flag to indicate if all data has been loaded ([Exhausted.isExhausted]).
 * - [Error]: Indicates that an error occurred during data loading, with an associated error message ([com.flixclusive.core.common.locale.UiText]).
 *
 * The class also provides convenience properties to check if the state is loading or done.
 */
sealed class PagingState {
    data object Idle : PagingState()

    data object Loading : PagingState()

    data object Exhausted : PagingState()

    data class Error(
        val error: UiText,
    ) : PagingState() {
        constructor(
            @StringRes errorId: Int
        ) : this(UiText.from(errorId))
        constructor(error: String) : this(UiText.from(error))
        constructor(exception: Throwable? = null) :
            this(
                exception?.localizedMessage?.let { UiText.from(it) }
                    ?: UiText.from(R.string.paging_state_default_error),
            )
    }

    val isIdle get() = this is Idle
    val isLoading get() = this is Loading
    val isExhausted get() = this is Exhausted
    val isError get() = this is Error
}
