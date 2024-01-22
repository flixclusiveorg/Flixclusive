package com.flixclusive.core.util.common.ui

enum class PagingState {
    LOADING,
    ERROR,
    PAGINATING,
    PAGINATING_EXHAUST,
    IDLE
}

fun PagingState.isLoading() = this == PagingState.LOADING || this == PagingState.PAGINATING
fun PagingState.isError() = this == PagingState.ERROR || this == PagingState.PAGINATING_EXHAUST