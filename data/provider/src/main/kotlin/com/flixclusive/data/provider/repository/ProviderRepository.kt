package com.flixclusive.data.provider.repository

import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.Flow

interface ProviderRepository {
    suspend fun install(provider: InstalledProvider)

    suspend fun uninstall(provider: InstalledProvider)

    suspend fun unload(id: String)

    suspend fun load(
        provider: Provider,
        classLoader: PathClassLoader,
        metadata: ProviderMetadata,
    )

    suspend fun getApi(id: String, ownerId: Int): ProviderApi?

    fun getMetadata(id: String): ProviderMetadata?

    fun getPlugin(id: String): Provider?

    suspend fun getConfig(id: String, ownerId: Int): InstalledProvider?

    fun getEnabledProvidersAsFlow(ownerId: Int): Flow<List<InstalledProvider>>

    suspend fun getEnabledProviders(ownerId: Int): List<InstalledProvider>

    suspend fun isEnabled(id: String, ownerId: Int): Boolean

    suspend fun getInstalledProviders(ownerId: Int): List<InstalledProvider>

    fun getInstalledProvidersAsFlow(ownerId: Int): Flow<List<InstalledProvider>>

    suspend fun getMaxSortOrder(ownerId: Int): Double

    suspend fun moveProvider(
        from: Int,
        to: Int,
        ownerId: Int,
    )

    suspend fun clearAll()

    suspend fun toggleProvider(id: String, ownerId: Int)
}
