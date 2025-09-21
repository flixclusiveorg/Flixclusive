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
        }
    }

    private fun handleAdd(id: String) {
        val provider = repository.getProviderMetadata(id)!!
        if (!providers.contains(provider)) {
            providers.add(provider)
        }
    }

    private fun handleRemove(id: String) {
        providers.removeIf { it.id == id }
    }
}
