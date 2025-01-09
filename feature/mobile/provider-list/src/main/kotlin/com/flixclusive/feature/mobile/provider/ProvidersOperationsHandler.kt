package com.flixclusive.feature.mobile.provider

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.data.provider.util.ListOperation
import com.flixclusive.model.datastore.user.ProviderFromPreferences
import com.flixclusive.model.provider.ProviderMetadata

class ProvidersOperationsHandler(
    private val repository: ProviderRepository,
    private val providers: SnapshotStateList<ProviderMetadata>,
) {
    fun handleOperations(operation: ListOperation<ProviderFromPreferences>) {
        when (operation) {
            is ListOperation.Add -> handleAdd(operation.item.id)
            is ListOperation.Clear -> handleClear()
            is ListOperation.Move -> handleMove(operation.from, operation.to)
            is ListOperation.Remove -> handleRemove(operation.item.id)
            is ListOperation.Replace -> Unit
        }
    }

    private fun handleAdd(id: String) {
        val provider = repository.getProviderMetadata(id)!!
        if (!providers.contains(provider)) {
            providers.add(provider)
        }
    }

    private fun handleClear() {
        providers.clear()
    }

    private fun handleMove(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (fromIndex !in providers.indices || toIndex !in providers.indices) return

        val provider = providers[fromIndex]
        providers.removeAt(fromIndex)
        providers.add(toIndex, provider)
    }

    private fun handleRemove(id: String) {
        providers.removeIf { it.id == id }
    }
}
