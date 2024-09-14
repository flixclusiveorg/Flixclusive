package com.flixclusive.core.ui.common.util

enum class PagingState {
    LOADING,
    ERROR,
    PAGINATING,
    PAGINATING_EXHAUST,
    IDLE;

    val isLoading
        get() = this == LOADING || this == PAGINATING
    val isError
        get() = this == ERROR || this == PAGINATING_EXHAUST
}