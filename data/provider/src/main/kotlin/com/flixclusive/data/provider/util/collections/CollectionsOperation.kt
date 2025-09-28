package com.flixclusive.data.provider.util.collections

import kotlinx.serialization.Serializable

/**
 * A sealed interface representing various operations that can be performed on async collections.
 *
 * It includes operations for both lists and maps, allowing for adding and removing items.
 *
 * @see ReactiveMap
 * @see ReactiveList
 * */
sealed interface CollectionsOperation<T> {
    @Serializable
    sealed class List<T> : CollectionsOperation<T> {
        data class Add<T>(
            val item: T,
        ) : List<T>()

        data class Remove<T>(
            val item: T,
        ) : List<T>()
    }

    @Serializable
    sealed class Map<K, V> : CollectionsOperation<V> {
        data class Add<K, V>(
            val key: K,
            val value: V,
        ) : Map<K, V>()

        data class Remove<K, V>(
            val key: K,
            val value: V? = null,
        ) : Map<K, V>()
    }
}
