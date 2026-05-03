package com.flixclusive.core.presentation.mobile.extensions

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Determines if pagination should occur based on the current scroll position.
 *
 * @param buffer The number of items when we should start loading more items before reaching the end.
 *
 * @return True if pagination should occur, false otherwise.
 * */
fun ScrollableState.shouldPaginate(buffer: Int = 6): Boolean {
    val (totalItemsCount, visibleItemsInfoSize, lastVisibleItemIndex) = when (this) {
        is LazyGridState -> Triple(
            layoutInfo.totalItemsCount,
            layoutInfo.visibleItemsInfo.size,
            layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        )

        is LazyListState -> Triple(
            layoutInfo.totalItemsCount,
            layoutInfo.visibleItemsInfo.size,
            layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        )

        else -> return false // Not a supported scrollable state
    }

    // If all items are visible (no scrolling needed), always paginate to fill the screen
    val allItemsVisible = visibleItemsInfoSize >= totalItemsCount

    // Paginate if near the end OR if the list doesn't fill the screen yet
    return allItemsVisible || lastVisibleItemIndex >= (totalItemsCount - buffer)
}

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
