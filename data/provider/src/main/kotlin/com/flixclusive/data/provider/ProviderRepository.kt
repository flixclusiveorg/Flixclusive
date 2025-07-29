package com.flixclusive.data.provider

import com.flixclusive.data.provider.util.CollectionsOperation
import com.flixclusive.model.datastore.user.ProviderFromPreferences
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.Provider
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.SharedFlow

interface ProviderRepository {
    suspend fun add(
        provider: Provider,
        classLoader: PathClassLoader,
        metadata: ProviderMetadata,
        preferenceItem: ProviderFromPreferences,
    )

    suspend fun addToPreferences(preferenceItem: ProviderFromPreferences)

    fun getProviderMetadata(id: String): ProviderMetadata?

    fun getProvider(id: String): Provider?

    fun getProviderFromPreferences(id: String): ProviderFromPreferences?

    fun getEnabledProviders(): List<ProviderMetadata>

    fun getProviders(): List<ProviderMetadata>

    fun getOrderedProviders(): List<ProviderMetadata>

    fun observe(): SharedFlow<CollectionsOperation.List<ProviderFromPreferences>>

    suspend fun moveProvider(
        fromIndex: Int,
        toIndex: Int,
    )

    suspend fun remove(id: String)

    suspend fun clearAll()

    suspend fun removeFromPreferences(id: String)

    suspend fun toggleProvider(id: String)
}
