package com.flixclusive.feature.mobile.provider.manage

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.core.datastore.model.user.ProviderFromPreferences
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.util.collections.CollectionsOperation
import com.flixclusive.data.provider.util.collections.CollectionsOperationHandler
import com.flixclusive.model.provider.ProviderMetadata

internal class ProvidersOperationsHandler(
    private val repository: ProviderRepository,
    private val providers: SnapshotStateList<ProviderMetadata>,
) : CollectionsOperationHandler<ProviderFromPreferences> {
    override fun handleOperations(operation: CollectionsOperation<ProviderFromPreferences>) {
        if (operation !is CollectionsOperation.List) return

        when (operation) {
            is CollectionsOperation.List.Add -> handleAdd(operation.item.id)
            is CollectionsOperation.List.Remove -> handleRemove(operation.item.id)
            is CollectionsOperation.List.Clear -> providers.clear()
            is CollectionsOperation.List.Move -> {
                providers.add(operation.to, providers.removeAt(operation.from))
            }
            is CollectionsOperation.List.Replace -> {
                val provider = repository.getMetadata(operation.item.id) ?: return
                providers[operation.index] = provider
            }
        }
    }

    private fun handleAdd(id: String) {
        val provider = repository.getMetadata(id)!!
        if (!providers.contains(provider)) {
            providers.add(provider)
        }
    }

    private fun handleRemove(id: String) {
        providers.removeIf { it.id == id }
    }
}
