package com.flixclusive.data.provider

import android.content.Context
import com.flixclusive.data.provider.util.ReactiveMap
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import dagger.hilt.android.qualifiers.ApplicationContext
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
        private val apis = ReactiveMap<String, ProviderApi>()

        fun observe() = apis.operations

        fun getAll() = apis.toList()

        fun getApis() = apis.values.toList()

        fun getApi(id: String) = apis[id]

        private suspend fun addApi(
            id: String,
            api: ProviderApi,
        ) = apis.add(id, api)

        suspend fun addApiFromProvider(
            id: String,
            provider: Provider,
        ) {
            val api = provider.getApi(context, client)
            addApi(id, api)
        }

        suspend fun addApiFromId(id: String) {
            val provider =
                providerRepository.getProvider(id)
                    ?: throw NullPointerException("Provider [$id] is not yet loaded!")

            addApiFromProvider(id, provider)
        }

        suspend fun removeApi(id: String) {
            apis.remove(id)
        }

        suspend fun clearAll() {
            apis.clear()
        }
    }
