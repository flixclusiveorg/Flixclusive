package com.flixclusive.data.provider

import android.content.Context
import com.flixclusive.data.provider.util.isNotUsable
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderApiRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val client: OkHttpClient,
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

        private fun addApi(
            id: String,
            api: ProviderApi,
        ) {
            apisAsStateFlow.update {
                val newMap = it.toMutableMap()
                newMap[id] = api
                newMap.toMap()
            }
        }

        fun addApiFromProvider(
            id: String,
            provider: Provider,
        ) {
            val api = provider.getApi(context, client)
            addApi(id, api)
        }

        fun addApiFromId(id: String) {
            val provider =
                providerRepository.getProvider(id)
                    ?: throw NullPointerException("Provider [$id] is not yet loaded!")

            addApiFromProvider(id, provider)
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
