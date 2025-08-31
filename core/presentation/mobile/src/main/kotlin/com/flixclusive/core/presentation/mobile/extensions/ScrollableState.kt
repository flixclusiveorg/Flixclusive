package com.flixclusive.core.presentation.mobile.extensions

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// TODO: Improve pagination logic

fun LazyGridState.shouldPaginate(toDeduct: Int = 6) = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -9) >= (layoutInfo.totalItemsCount - toDeduct) || !canScrollForward

fun LazyListState.shouldPaginate(toDeduct: Int = 6) = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -9) >= (layoutInfo.totalItemsCount - toDeduct) || !canScrollForward

@Composable
fun LazyGridState.isAtTop(): State<Boolean> {
    return remember(this) {
        derivedStateOf {
            firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
        }
    }
}

@Composable
fun LazyGridState.isScrollingUp(): State<Boolean> {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }
}

@Composable
fun LazyListState.isAtTop(): State<Boolean> {
    return remember(this) {
        derivedStateOf {
            firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
        }
    }
}

@Composable
fun LazyListState.isScrollingUp(): State<Boolean> {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }
}
