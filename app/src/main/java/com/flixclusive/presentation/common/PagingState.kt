package com.flixclusive.presentation.common

import androidx.compose.foundation.lazy.grid.LazyGridState

enum class PagingState {
    LOADING,
    ERROR,
    PAGINATING,
    PAGINATING_EXHAUST,
    IDLE
}

fun LazyGridState.shouldPaginate() = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -9) >= (layoutInfo.totalItemsCount - 6) || !canScrollForward