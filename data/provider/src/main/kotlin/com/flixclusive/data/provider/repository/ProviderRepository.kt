package com.flixclusive.data.provider.repository

import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.provider.ProviderPlugin
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.Flow
import java.util.Date

data class ProviderResponseWrapper(
    val provider: InstalledProvider,
    val plugin: ProviderPlugin?,
    val metadata: ProviderMetadata?,
) {
    val id: String get() = provider.id
    val name: String? get() = metadata?.name

    val logoUrl: String? get() = metadata?.iconUrl
    val versionName: String? get() = metadata?.versionName
    val versionCode: Long? get() = metadata?.versionCode

    val status: ProviderStatus? get() = metadata?.status

    val sortOrder: Double get() = provider.sortOrder
    val createdAt: Date get() = provider.createdAt

    val isEnabled: Boolean get() = provider.isEnabled

    val isDebug: Boolean get() = provider.isDebug
}

interface ProviderRepository {
    suspend fun install(
        provider: InstalledProvider,
        metadata: ProviderMetadata
    )

    suspend fun uninstall(provider: InstalledProvider)

    suspend fun unload(id: String)

    suspend fun load(
        provider: ProviderPlugin,
        classLoader: PathClassLoader,
        metadata: ProviderMetadata,
    )

    suspend fun getProvider(id: String, ownerId: String): ProviderResponseWrapper?

    fun getEnabledProvidersAsFlow(ownerId: String): Flow<List<ProviderResponseWrapper>>

    suspend fun getEnabledProviders(ownerId: String): List<ProviderResponseWrapper>

    suspend fun isEnabled(id: String, ownerId: String): Boolean

    suspend fun getProviders(ownerId: String): List<ProviderResponseWrapper>

    fun getProvidersAsFlow(ownerId: String): Flow<List<ProviderResponseWrapper>>

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
