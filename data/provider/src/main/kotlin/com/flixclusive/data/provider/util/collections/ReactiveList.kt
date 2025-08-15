package com.flixclusive.data.provider.util.collections

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

@Serializable
internal class ReactiveList<T>(
    private val list: ArrayList<T> = ArrayList<T>(),
) : List<T> by list {
    private val _operations = MutableSharedFlow<CollectionsOperation.List<T>>()
    val operations get() = _operations.asSharedFlow()

    private val mutex = Mutex()

    suspend fun add(item: T) =
        mutex.withLock {
            list.add(item)
            _operations.emit(CollectionsOperation.List.Add(item))
        }

    suspend fun replaceAt(
        index: Int,
        item: T,
    ) = mutex.withLock {
        list[index] = item
    }

    suspend fun removeIf(filter: (T) -> Boolean): Boolean =
        mutex.withLock {
            val item = list.find(filter)

            if (item != null) {
                _operations.emit(CollectionsOperation.List.Remove(item))
            }

            return list.remove(item)
        }

    suspend fun move(
        from: Int,
        to: Int,
    ) = mutex.withLock {
        if (from !in indices || to !in indices) return

        val item = list.removeAt(from)
        list.add(to, item)
    }

    suspend fun clear() =
        mutex.withLock {
            list.clear()
        }
}
