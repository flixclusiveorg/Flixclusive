package com.flixclusive.data.provider.repository

import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.Flow

interface ProviderRepository {
    suspend fun install(
        provider: InstalledProvider,
        metadata: ProviderMetadata
    )

    suspend fun uninstall(provider: InstalledProvider)

    suspend fun unload(id: String)

    suspend fun load(
        provider: Provider,
        classLoader: PathClassLoader,
        metadata: ProviderMetadata,
    )

    suspend fun getApi(id: String, ownerId: String): ProviderApi?

    fun getMetadata(id: String): ProviderMetadata?

    fun getPlugin(id: String): Provider?

    suspend fun getInstalledProvider(id: String, ownerId: String): InstalledProvider?

    fun getEnabledProvidersAsFlow(ownerId: String): Flow<List<InstalledProvider>>

    suspend fun getEnabledProviders(ownerId: String): List<InstalledProvider>

    suspend fun isEnabled(id: String, ownerId: String): Boolean

    suspend fun getInstalledProviders(ownerId: String): List<InstalledProvider>

    fun getInstalledProvidersAsFlow(ownerId: String): Flow<List<InstalledProvider>>

    suspend fun getMaxSortOrder(ownerId: String): Double

    suspend fun reorderPosition(
        moved: InstalledProvider,
        before: InstalledProvider?,
        after: InstalledProvider?,
    )

    suspend fun renormalizePositions(ownerId: String)

    suspend fun clearAll()

    suspend fun toggleProvider(id: String, ownerId: String)
}
