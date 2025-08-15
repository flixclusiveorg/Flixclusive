package com.flixclusive.domain.provider.util.extensions

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// TODO: Put this in a more appropriate module/package

/**
 *
 * Add a new item to the list in a [MutableStateFlow].
 * */
fun <T> MutableStateFlow<List<T>>.add(result: T) {
    value = value + result
}

/**
 *
 * Update an existing item in a list in a [MutableStateFlow].
 * If the item is not found, it will not change the list.
 *
 * If the item is not found, it will not change the list.
 *
 * @param index The index of the item to update.
 * @param result The new item to replace the existing one.
 * */
fun <T> MutableStateFlow<List<T>>.replace(index: Int, result: T) {
    // Use `update` function to ensure that the value will be atomic
    update {
        if (index in it.indices) {
            it.toMutableList().apply {
                this[index] = result
            }.toList()
        } else {
            it // If index is out of bounds, return the original list
        }
    }
}
