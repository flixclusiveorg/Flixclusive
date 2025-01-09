package com.flixclusive.data.provider

import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.data.provider.util.ListOperation
import com.flixclusive.data.provider.util.ReactiveList
import com.flixclusive.data.provider.util.isNotUsable
import com.flixclusive.model.datastore.user.ProviderFromPreferences
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.Provider
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.component1
import kotlin.collections.component2

@Singleton
class ProviderRepository
    @Inject
    constructor(
        private val dataStoreManager: DataStoreManager,
    ) {
        private val providerMetadata = LinkedHashMap<String, ProviderMetadata>()
        private val providerPositions = ReactiveList<ProviderFromPreferences>()

        /** Map containing all loaded provider classes  */
        private val providerInstances: MutableMap<String, Provider> =
            Collections.synchronizedMap(LinkedHashMap())

        // TODO: Make this public for crash log purposes
        private val classLoaders: MutableMap<String, PathClassLoader> =
            Collections.synchronizedMap(HashMap())

        suspend fun add(
            provider: Provider,
            classLoader: PathClassLoader,
            metadata: ProviderMetadata,
            preferenceItem: ProviderFromPreferences,
        ) {
            classLoaders[metadata.id] = classLoader
            providerInstances[metadata.id] = provider
            providerMetadata[metadata.id] = metadata

            addToPreferences(preferenceItem = preferenceItem)
        }

        suspend fun addToPreferences(preferenceItem: ProviderFromPreferences) {
            providerPositions.add(preferenceItem)
            saveToPreferences()
        }

        fun getProviderMetadata(id: String): ProviderMetadata? = providerMetadata[id]

        fun getProvider(id: String): Provider? = providerInstances[id]

        fun getProviderFromPreferences(id: String): ProviderFromPreferences? = providerPositions.find { it.id == id }

        fun getEnabledProviders(): List<ProviderMetadata> {
            return providerMetadata.mapNotNull { (id, api) ->
                val metadata = getProviderMetadata(id = id)
                val preferenceItem = getProviderFromPreferences(id = id)
                if (metadata == null) return@mapNotNull null
                if (preferenceItem == null) return@mapNotNull null

                if (!metadata.isNotUsable && !preferenceItem.isDisabled) {
                    return@mapNotNull api
                }

                null
            }
        }

        fun getProviders(): List<ProviderMetadata> {
            return providerMetadata.values.toList()
        }

        fun getOrderedProviders(): List<ProviderMetadata> {
            return providerPositions.mapNotNull { item ->
                providerMetadata[item.id]
            }
        }

        fun observePositions(): SharedFlow<ListOperation<ProviderFromPreferences>> = providerPositions.operations

        suspend fun moveProvider(
            fromIndex: Int,
            toIndex: Int,
        ) {
            providerPositions.move(fromIndex, toIndex)
            saveToPreferences()
        }

        internal suspend fun remove(id: String) {
            providerInstances.remove(id)
            classLoaders.remove(id)
            providerMetadata.remove(id)
            removeFromPreferences(id)
        }

        internal suspend fun removeFromPreferences(id: String) {
            providerPositions.removeIf { it.id == id }
            saveToPreferences()
        }

        suspend fun toggleProvider(id: String) {
            val indexOfProvider =
                providerPositions.indexOfFirst { provider ->
                    provider.id == id
                }

            val provider = providerPositions[indexOfProvider]
            providerPositions.replaceAt(
                index = indexOfProvider,
                item = provider.copy(isDisabled = !provider.isDisabled),
            )

            saveToPreferences()
        }

        private suspend fun saveToPreferences() {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
                it.copy(providers = providerPositions)
            }
        }

        internal suspend fun initializeOrder() {
            val preferences =
                dataStoreManager
                    .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                    .first()

            preferences.providers.forEach(providerPositions::add)
        }
    }
