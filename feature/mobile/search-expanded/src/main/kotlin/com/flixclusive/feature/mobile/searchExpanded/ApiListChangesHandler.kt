package com.flixclusive.feature.mobile.searchExpanded

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.data.provider.util.collections.CollectionsOperation
import com.flixclusive.data.provider.util.collections.CollectionsOperationHandler
import com.flixclusive.provider.ProviderApi

internal class ApiListChangesHandler(
    private val apis: SnapshotStateList<ProviderApi>,
) : CollectionsOperationHandler<ProviderApi> {
    override fun handleOperations(operation: CollectionsOperation<ProviderApi>) {
        if (operation !is CollectionsOperation.Map<*, *>) return

        when (operation) {
            is CollectionsOperation.Map.Add<*, *> -> handleAdd(operation.value as ProviderApi)
            is CollectionsOperation.Map.Remove<*, *> -> handleRemove(operation.value as ProviderApi)
        }
    }

    private fun handleAdd(api: ProviderApi) {
        var hasChanges = false
        if (!apis.contains(api)) {
            hasChanges = true
            apis.add(api)
        }

        if (hasChanges) {
            apis.sortBy { sorter(it) }
        }
    }

    private fun handleRemove(api: ProviderApi) {
        apis.removeIf { it == api }
    }

    private fun sorter(api: ProviderApi): String {
        var name = safeCall("Can't get provider name, falling back to class name instead...") {
            api.provider.name
        }

        if (name.isNullOrEmpty()) {
            name = api::class.java.simpleName
        }

        return name!!
    }
}
