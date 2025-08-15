package com.flixclusive.core.common.pagination

/**
 * Indicates the current state of a paginated list.
 * This is used to manage the UI state during pagination operations.
 * @property LOADING Indicates that the initial data is being loaded.
 * @property ERROR Indicates that an error occurred while loading data.
 * @property PAGINATING Indicates that more data is being loaded.
 * @property EXHAUSTED Indicates that there are no more items to load.
 * @property IDLE Indicates that the list is idle and no loading is in progress.
 * */
enum class PagingState {
    LOADING,
    ERROR,
    PAGINATING,
    EXHAUSTED,
    IDLE,
    ;

    val isLoading
        get() = this == LOADING || this == PAGINATING
    val isError
        get() = this == ERROR || this == EXHAUSTED
}
