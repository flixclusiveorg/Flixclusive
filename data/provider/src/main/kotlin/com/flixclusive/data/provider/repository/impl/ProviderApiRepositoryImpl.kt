package com.flixclusive.data.provider.repository.impl

import android.content.Context
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.util.collections.ReactiveMap
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ProviderApiRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val client: OkHttpClient,
        private val providerRepository: ProviderRepository,
    ) : ProviderApiRepository {
        private val apis = ReactiveMap<String, ProviderApi>()

        override fun observe() = apis.operations

        override fun getAll() = apis.toList()

        override fun getApis() = apis.values.toList()

        override fun getApi(id: String) = apis[id]

        private suspend fun addApi(
            id: String,
            api: ProviderApi,
        ) = apis.add(id, api)

        override suspend fun addApiFromProvider(
            id: String,
            provider: Provider,
        ) {
            val api = provider.getApi(context, client)
            addApi(id, api)
        }

        override suspend fun addApiFromId(id: String) {
            val provider =
                providerRepository.getProvider(id)
                    ?: throw NullPointerException("Provider [$id] is not yet loaded!")

            addApiFromProvider(id, provider)
        }

        override suspend fun removeApi(id: String) {
            apis.remove(id)
        }

        override suspend fun clearAll() {
            apis.clear()
        }
    }
