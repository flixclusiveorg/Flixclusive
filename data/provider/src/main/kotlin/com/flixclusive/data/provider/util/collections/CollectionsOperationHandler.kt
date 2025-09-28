package com.flixclusive.data.provider.util.collections

/**
 * A handler interface for processing operations on async collections.
 *
 * @see ReactiveMap
 * @see ReactiveList
 * */
interface CollectionsOperationHandler<T> {
    fun handleOperations(operation: CollectionsOperation<T>)
}
