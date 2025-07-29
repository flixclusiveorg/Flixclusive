package com.flixclusive.data.provider

import com.flixclusive.data.provider.util.CollectionsOperation
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import kotlinx.coroutines.flow.SharedFlow

interface ProviderApiRepository {
    fun observe(): SharedFlow<CollectionsOperation.Map<String, ProviderApi>>

    fun getAll(): List<Pair<String, ProviderApi>>

    fun getApis(): List<ProviderApi>

    fun getApi(id: String): ProviderApi?

    suspend fun addApiFromProvider(
        id: String,
        provider: Provider,
    )

    suspend fun addApiFromId(id: String)

    suspend fun removeApi(id: String)

    suspend fun clearAll()
}
