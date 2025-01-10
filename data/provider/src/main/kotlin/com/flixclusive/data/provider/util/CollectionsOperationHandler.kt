package com.flixclusive.data.provider.util

interface CollectionsOperationHandler<T> {
    fun handleOperations(operation: CollectionsOperation<T>)
}
