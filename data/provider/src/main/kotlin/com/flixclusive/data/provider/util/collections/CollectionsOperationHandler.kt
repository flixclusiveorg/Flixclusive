package com.flixclusive.data.provider.util.collections

interface CollectionsOperationHandler<T> {
    fun handleOperations(operation: CollectionsOperation<T>)
}
