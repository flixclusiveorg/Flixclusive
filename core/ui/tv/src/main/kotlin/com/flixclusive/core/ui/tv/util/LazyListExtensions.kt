package com.flixclusive.core.ui.tv.util

import androidx.tv.foundation.lazy.grid.TvLazyGridState
import androidx.tv.foundation.lazy.list.TvLazyListState

fun TvLazyGridState.shouldPaginate(toDeduct: Int = 6) = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -9) >= (layoutInfo.totalItemsCount - toDeduct)

fun TvLazyListState.shouldPaginate(toDeduct: Int = 6) = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -9) >= (layoutInfo.totalItemsCount - toDeduct)
