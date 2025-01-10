package com.flixclusive.data.provider.util

sealed interface CollectionsOperation<T> {
    sealed class List<T> : CollectionsOperation<T> {
        data class Add<T>(
            val item: T,
        ) : List<T>()

        data class Remove<T>(
            val item: T,
        ) : List<T>()
    }

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
