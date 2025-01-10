package com.flixclusive.data.provider.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

@Serializable
internal class ReactiveMap<K, V>(
    private val list: HashMap<K, V> = HashMap<K, V>(),
) : Map<K, V> by list {
    private val _operations = MutableSharedFlow<CollectionsOperation.Map<K, V>>()
    val operations get() = _operations.asSharedFlow()

    private val mutex = Mutex()

    suspend fun add(
        key: K,
        value: V,
    ) = mutex.withLock {
        list[key] = value
        _operations.emit(CollectionsOperation.Map<K, V>.Add(key, value))
    }

    suspend fun remove(key: K) {
        mutex.withLock {
            val item = list.remove(key)
            _operations.emit(CollectionsOperation.Map<K, V>.Remove(key, item))
        }
    }

    suspend fun clear() =
        mutex.withLock {
            list.clear()
        }
}
