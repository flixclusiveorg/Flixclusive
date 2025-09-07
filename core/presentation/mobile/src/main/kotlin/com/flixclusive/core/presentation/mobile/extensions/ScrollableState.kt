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

/**
 * Determines if pagination should occur based on the current scroll position.
 *
 * @param buffer The number of items when we should start loading more items before reaching the end.
 *
 * @return True if pagination should occur, false otherwise.
 * */
fun LazyGridState.shouldPaginate(buffer: Int = 6): Boolean {
    // Get the total number of items in the list
    val totalItemsCount = layoutInfo.totalItemsCount
    // Get the index of the last visible item
    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    // Check if we have scrolled near the end of the list and more items should be loaded
    return lastVisibleItemIndex >= (totalItemsCount - buffer)
}

/**
 * Determines if pagination should occur based on the current scroll position.
 *
 * @param buffer The number of items when we should start loading more items before reaching the end.
 *
 * @return True if pagination should occur, false otherwise.
 * */
fun LazyListState.shouldPaginate(buffer: Int = 6): Boolean {
    // Get the total number of items in the list
    val totalItemsCount = layoutInfo.totalItemsCount
    // Get the index of the last visible item
    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    // Check if we have scrolled near the end of the list and more items should be loaded
    return lastVisibleItemIndex >= (totalItemsCount - buffer)
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
