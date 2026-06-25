package com.flixclusive.data.provider.repository

import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.data.provider.ProviderCapability
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

    val versionName: String? get() = metadata?.versionName
    val versionCode: Long? get() = metadata?.versionCode

    val status: ProviderStatus? get() = metadata?.status

    val createdAt: Date get() = provider.createdAt

    val isCatalogEnabled: Boolean get() = provider.isCatalogEnabled
    val isCrossMatchEnabled: Boolean get() = provider.isCrossMatchEnabled
    val isMediaLinkEnabled: Boolean get() = provider.isMediaLinkEnabled
    val isMetadataEnabled: Boolean get() = provider.isMetadataEnabled
    val isSearchEnabled: Boolean get() = provider.isSearchEnabled
    val isTrackerEnabled: Boolean get() = provider.isTrackerEnabled

    val isDebug: Boolean get() = provider.isDebug
}

interface ProviderRepository {
    suspend fun install(
        provider: InstalledProvider,
        metadata: ProviderMetadata
    )

    suspend fun uninstall(provider: InstalledProvider)

    suspend fun load(
        provider: ProviderPlugin,
        classLoader: PathClassLoader,
        metadata: ProviderMetadata,
    )

    suspend fun getProvider(id: String, ownerId: String): ProviderResponseWrapper?

    fun getProvidersWithCapabilityAsFlow(
        ownerId: String,
        capability: ProviderCapability
    ): Flow<List<ProviderResponseWrapper>>

    suspend fun getProvidersWithCapability(
        ownerId: String,
        capability: ProviderCapability
    ): List<ProviderResponseWrapper>

    suspend fun getProviders(ownerId: String): List<ProviderResponseWrapper>

    fun getProvidersAsFlow(ownerId: String): Flow<List<ProviderResponseWrapper>>

    suspend fun clearAll()

    suspend fun setCapabilityEnabled(id: String, ownerId: String, capability: ProviderCapability, enabled: Boolean)

    fun getProviderAsFlow(id: String, ownerId: String): Flow<ProviderResponseWrapper?>
}
