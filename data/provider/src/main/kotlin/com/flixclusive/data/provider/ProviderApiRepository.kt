package com.flixclusive.data.provider

import com.flixclusive.data.provider.util.isNotUsable
import com.flixclusive.provider.ProviderApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderApiRepository
    @Inject
    constructor(
        private val providerRepository: ProviderRepository,
    ) {
        private val apisAsStateFlow = MutableStateFlow<Map<String, ProviderApi>>(mapOf())

        fun getEnabledApisAsFlow(): Flow<List<ProviderApi>> {
            return apisAsStateFlow.map {
                it.mapNotNull { (id, api) ->
                    val metadata = providerRepository.getProviderMetadata(id = id)
                    val preferenceItem = providerRepository.getProviderFromPreferences(id = id)
                    if (metadata == null) return@mapNotNull null
                    if (preferenceItem == null) return@mapNotNull null

                    if (!metadata.isNotUsable && !preferenceItem.isDisabled) {
                        return@mapNotNull api
                    }

                    null
                }
            }
        }

        fun getAll() = apisAsStateFlow.value.map { it.toPair() } as ArrayList

        fun getApi(id: String) = apisAsStateFlow.value[id]

        fun addApi(
            id: String,
            api: ProviderApi,
        ) {
            apisAsStateFlow.update {
                val newMap = it.toMutableMap()
                newMap[id] = api
                newMap.toMap()
            }
        }

        /**
         *
         * Removes all providers registered to the given provider name.
         * */
        fun removeApi(id: String) {
            apisAsStateFlow.update {
                val newMap = it.toMutableMap()
                newMap.remove(id)
                newMap.toMap()
            }
        }

        fun clearAll() {
            apisAsStateFlow.value = mapOf()
        }
    }
