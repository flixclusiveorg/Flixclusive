package com.flixclusive.data.provider

import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.data.provider.util.isNotUsable
import com.flixclusive.model.datastore.user.ProviderFromPreferences
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.Provider
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
        private val providersAsStateFlow = MutableStateFlow(mapOf<String, ProviderMetadata>())
        private val orderListAsStateFlow = MutableStateFlow(listOf<ProviderFromPreferences>())

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
            providersAsStateFlow.update {
                val newMap = it.toMutableMap()
                newMap[metadata.id] = metadata
                newMap.toMap()
            }

            addToPreferences(preferenceItem = preferenceItem)
        }

        suspend fun addToPreferences(preferenceItem: ProviderFromPreferences) {
            orderListAsStateFlow.update {
                val newList = it.toMutableList()
                if (!newList.contains(preferenceItem)) {
                    newList.add(preferenceItem)
                }
                newList.toList()
            }

            saveToPreferences()
        }

        fun getProviderMetadata(id: String): ProviderMetadata? = providersAsStateFlow.value[id]

        fun getProvider(id: String): Provider? = providerInstances[id]

        fun getProviderFromPreferences(id: String): ProviderFromPreferences? =
            orderListAsStateFlow.value.find { it.id == id }

        fun getEnabledProvidersAsFlow(): Flow<List<ProviderMetadata>> {
            return providersAsStateFlow.map {
                it.mapNotNull { (id, api) ->
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
        }

        suspend fun getProviders(): List<ProviderMetadata> {
            return providersAsStateFlow.first().values.toList()
        }

        suspend fun getOrderedProviders(): List<ProviderMetadata> {
            return orderListAsStateFlow
                .map { list ->
                    list.mapNotNull { item ->
                        providersAsStateFlow.value[item.id]
                    }
                }.first()
        }

        fun getOrderedProvidersAsFlow(): Flow<List<ProviderMetadata>> {
            return orderListAsStateFlow.map { list ->
                list.mapNotNull { item ->
                    providersAsStateFlow.value[item.id]
                }
            }
        }

        suspend fun moveProvider(
            fromIndex: Int,
            toIndex: Int,
        ) {
            orderListAsStateFlow.update {
                val newList = it.toMutableList()
                if (fromIndex !in newList.indices && toIndex !in newList.indices) {
                    return@update it
                }

                val id = newList.removeAt(fromIndex)
                newList.add(toIndex, id)
                newList.toList()
            }

            saveToPreferences()
        }

        internal suspend fun remove(id: String) {
            providerInstances.remove(id)
            classLoaders.remove(id)
            providersAsStateFlow.update {
                if (it.size == 1) return@update mapOf()

                val newMap = it.toMutableMap()
                newMap.remove(id)
                newMap.toMap()
            }
            removeFromPreferences(id)
        }

        internal suspend fun removeFromPreferences(id: String) {
            orderListAsStateFlow.update {
                val newList = it.toMutableList()
                newList.removeIf { it.id == id }
                newList.toList()
            }

            saveToPreferences()
        }

        suspend fun setEnabled(
            id: String,
            isDisabled: Boolean,
        ) {
            orderListAsStateFlow.update {
                val newList = it.toMutableList()

                val indexOfProvider =
                    newList.indexOfFirst { provider -> provider.id == id }

                val provider = newList[indexOfProvider]
                newList[indexOfProvider] = provider.copy(isDisabled = isDisabled == true)

                newList.toList()
            }

            saveToPreferences()
        }

        private suspend fun saveToPreferences() {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
                it.copy(providers = orderListAsStateFlow.value)
            }
        }

        internal suspend fun initializeOrder() {
            val preferences =
                dataStoreManager
                    .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                    .first()

            orderListAsStateFlow.update {
                val newList = it.toMutableList()
                preferences.providers.forEach(newList::add)
                newList.toList()
            }
        }
    }
