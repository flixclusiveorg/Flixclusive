package com.flixclusive.core.util.common.ui

enum class PagingState {
    LOADING,
    ERROR,
    PAGINATING,
    PAGINATING_EXHAUST,
    IDLE
}

val PagingState.isLoading
    get() = this == PagingState.LOADING || this == PagingState.PAGINATING
val PagingState.isError
    get() = this == PagingState.ERROR || this == PagingState.PAGINATING_EXHAUST