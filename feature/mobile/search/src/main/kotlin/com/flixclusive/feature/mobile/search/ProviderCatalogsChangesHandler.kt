package com.flixclusive.feature.mobile.search

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.util.fastForEach
import com.flixclusive.data.provider.util.CollectionsOperation
import com.flixclusive.data.provider.util.CollectionsOperationHandler
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.provider.ProviderApi

internal class ProviderCatalogsChangesHandler(
    private val catalogs: SnapshotStateList<ProviderCatalog>,
) : CollectionsOperationHandler<ProviderApi> {
    override fun handleOperations(operation: CollectionsOperation<ProviderApi>) {
        if (operation !is CollectionsOperation.Map<*, *>) return

        when (operation) {
            is CollectionsOperation.Map.Add<*, *> -> handleAdd(operation.value as ProviderApi)
            is CollectionsOperation.Map.Remove<*, *> -> handleRemove(operation.key as String)
        }
    }

    private fun handleAdd(api: ProviderApi) {
        var hasChanges = false
        api.catalogs.fastForEach {
            if (!catalogs.contains(it)) {
                hasChanges = true
                catalogs.add(it)
            }
        }

        if (hasChanges) {
            catalogs.sortBy { it.name }
        }
    }

    private fun handleRemove(id: String) {
        val hasChanges = catalogs.removeAll { it.providerId == id }
        if (hasChanges) {
            catalogs.sortBy { it.name }
        }
    }
}
