package com.flixclusive.domain.catalog.util

import com.flixclusive.data.provider.util.collections.CollectionsOperation
import com.flixclusive.data.provider.util.collections.CollectionsOperationHandler
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.provider.ProviderApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ProviderCatalogsOperationsHandler : CollectionsOperationHandler<ProviderApi> {
    private val _catalogs = MutableStateFlow<Set<ProviderCatalog>>(setOf())
    val catalogs = _catalogs.asStateFlow()

    override fun handleOperations(operation: CollectionsOperation<ProviderApi>) {
        if (operation !is CollectionsOperation.Map<*, *>) return

        when (operation) {
            is CollectionsOperation.Map.Add<*, *> -> {
                _catalogs.value = _catalogs.value + (operation.value as ProviderApi).catalogs
            }
            is CollectionsOperation.Map.Remove<*, *> -> {
                _catalogs.value = _catalogs.value - (operation.value as ProviderApi).catalogs
            }
        }
    }
}
