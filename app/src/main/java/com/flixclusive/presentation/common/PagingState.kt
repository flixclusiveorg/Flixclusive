package com.flixclusive.presentation.common

enum class PagingState {
    LOADING,
    ERROR,
    PAGINATING,
    PAGINATING_EXHAUST,
    IDLE,
    NON_PAGEABLE
}