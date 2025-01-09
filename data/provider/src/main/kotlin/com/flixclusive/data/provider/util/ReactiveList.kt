package com.flixclusive.data.provider.util

import com.flixclusive.core.util.coroutines.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
internal class ReactiveList<T>(
    private val scope: CoroutineScope = AppDispatchers.IO.scope,
    private val list: ArrayList<T> = ArrayList<T>(),
) : List<T> by list {
    private val _operations = MutableSharedFlow<ListOperation<T>>()
    val operations get() = _operations.asSharedFlow()

    fun add(item: T) {
        scope.launch {
            _operations.emit(ListOperation.Add(item))
            list.add(item)
        }
    }

    fun replaceAt(
        index: Int,
        item: T,
    ) {
        scope.launch {
            list[index] = item
            _operations.emit(ListOperation.Replace(index, item))
        }
    }

    fun removeIf(filter: (T) -> Boolean): Boolean {
        val item = list.find(filter)

        if (item != null) {
            scope.launch {
                _operations.emit(ListOperation.Remove(item))
            }
        }

        return list.remove(item)
    }

    fun move(
        from: Int,
        to: Int,
    ) {
        scope.launch {
            if (from !in indices && to !in indices) return@launch

            val item = list.removeAt(from)
            list.add(to, item)

            _operations.emit(ListOperation.Move(from, to))
        }
    }

    fun clear() {
        scope.launch {
            _operations.emit(ListOperation.Clear())
            list.clear()
        }
    }
}

sealed class ListOperation<T> {
    data class Add<T>(
        val item: T,
    ) : ListOperation<T>()

    data class Replace<T>(
        val index: Int,
        val item: T,
    ) : ListOperation<T>()

    data class Remove<T>(
        val item: T,
    ) : ListOperation<T>()

    data class Move<T>(
        val from: Int,
        val to: Int,
    ) : ListOperation<T>()

    class Clear<T> : ListOperation<T>()
}
