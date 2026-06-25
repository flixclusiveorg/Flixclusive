package com.flixclusive.data.provider.repository.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.provider.InstalledProviderDao
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.repository.ProviderResponseWrapper
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.ProviderPlugin
import dagger.hilt.android.qualifiers.ApplicationContext
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject

internal class ProviderRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val installedProviderDao: InstalledProviderDao,
    private val appDispatchers: AppDispatchers
) : ProviderRepository {
    private companion object {
        operator fun <K, V> MutableStateFlow<Map<K, V>>.set(key: K, value: V) {
            update { it + (key to value) }
        }

        operator fun <K, V> MutableStateFlow<Map<K, V>>.get(key: K): V? = value[key]

        fun <K, V> MutableStateFlow<Map<K, V>>.clear() {
            update { emptyMap() }
        }

        fun <K, V> MutableStateFlow<Map<K, V>>.remove(key: K) {
            update { it - key }
        }
    }

    private val pluginsMap = MutableStateFlow<Map<String, ProviderPlugin>>(emptyMap())
    private val metadataMap = MutableStateFlow<Map<String, ProviderMetadata>>(emptyMap())

    // TODO: Make this public for crash log purposes
    private val classLoadersMap: MutableMap<String, PathClassLoader> =
        Collections.synchronizedMap(HashMap())

    override suspend fun load(
        provider: ProviderPlugin,
        classLoader: PathClassLoader,
        metadata: ProviderMetadata,
    ) {
        classLoadersMap[metadata.id] = classLoader
        pluginsMap[metadata.id] = provider
        metadataMap[metadata.id] = metadata
    }

    override suspend fun install(
        provider: InstalledProvider,
        metadata: ProviderMetadata
    ) = withContext(appDispatchers.io) {
        installedProviderDao.insert(provider)
        metadataMap[provider.id] = metadata
    }

    override suspend fun uninstall(provider: InstalledProvider) = withContext(appDispatchers.io) {
        withContext(appDispatchers.io) {
            pluginsMap[provider.id]?.onUnload(context)
        }

        pluginsMap.remove(provider.id)
        metadataMap.remove(provider.id)
        classLoadersMap.remove(provider.id)

        installedProviderDao.delete(provider)
    }

    override suspend fun getProvider(
        id: String,
        ownerId: String
    ): ProviderResponseWrapper? {
        return withContext(appDispatchers.io) {
            val installedProvider = installedProviderDao.get(id, ownerId)
            val plugin = pluginsMap[id]
            val metadata = metadataMap[id]

            if (installedProvider == null) {
                return@withContext null
            }

            ProviderResponseWrapper(
                provider = installedProvider,
                plugin = plugin,
                metadata = metadata
            )
        }
    }

    override suspend fun getProviders(ownerId: String) = withContext(appDispatchers.io) {
        installedProviderDao.getAll(ownerId).map {
            val plugin = pluginsMap[it.id]
            val metadata = metadataMap[it.id]

            ProviderResponseWrapper(
                provider = it,
                plugin = plugin,
                metadata = metadata
            )
        }
    }

    override fun getProvidersAsFlow(ownerId: String) = combine(
        installedProviderDao.getAllAsFlow(ownerId),
        pluginsMap,
        metadataMap
    ) { providers, plugins, metadata ->
        providers.map { provider ->
            ProviderResponseWrapper(
                provider = provider,
                plugin = plugins[provider.id],
                metadata = metadata[provider.id]
            )
        }
    }

    override fun getProvidersWithCapabilityAsFlow(
        ownerId: String,
        capability: ProviderCapability
    ): Flow<List<ProviderResponseWrapper>> {
        return getProvidersAsFlow(ownerId).mapLatest { providers ->
            providers.filter { wrapper ->
                when (capability) {
                    ProviderCapability.CATALOG -> wrapper.plugin?.getCatalogApi(context) != null
                    ProviderCapability.CROSS_MATCH -> wrapper.plugin?.getCrossMatchApi(context) != null
                    ProviderCapability.MEDIA_LINK -> wrapper.plugin?.getMediaLinkApi(context) != null
                    ProviderCapability.METADATA -> wrapper.plugin?.getMetadataApi(context) != null
                    ProviderCapability.SEARCH -> wrapper.plugin?.getSearchApi(context) != null
                    ProviderCapability.TRACKER -> wrapper.plugin?.getTrackerApi(context) != null
                }
            }
        }
    }

    override suspend fun getProvidersWithCapability(
        ownerId: String,
        capability: ProviderCapability
    ): List<ProviderResponseWrapper> {
        return getProviders(ownerId).filter { wrapper ->
            when (capability) {
                ProviderCapability.CATALOG -> wrapper.plugin?.getCatalogApi(context) != null
                ProviderCapability.CROSS_MATCH -> wrapper.plugin?.getCrossMatchApi(context) != null
                ProviderCapability.MEDIA_LINK -> wrapper.plugin?.getMediaLinkApi(context) != null
                ProviderCapability.METADATA -> wrapper.plugin?.getMetadataApi(context) != null
                ProviderCapability.SEARCH -> wrapper.plugin?.getSearchApi(context) != null
                ProviderCapability.TRACKER -> wrapper.plugin?.getTrackerApi(context) != null
            }
        }
    }

    override suspend fun clearAll() {
        pluginsMap.clear()
        metadataMap.clear()
        classLoadersMap.clear()
    }

    override suspend fun setCapabilityEnabled(
        id: String,
        ownerId: String,
        capability: ProviderCapability,
        enabled: Boolean
    ) {
        when (capability) {
            ProviderCapability.CATALOG -> installedProviderDao.setCatalogEnabled(id, ownerId, enabled)
            ProviderCapability.CROSS_MATCH -> installedProviderDao.setCrossMatchEnabled(id, ownerId, enabled)
            ProviderCapability.MEDIA_LINK -> installedProviderDao.setMediaLinkEnabled(id, ownerId, enabled)
            ProviderCapability.METADATA -> installedProviderDao.setMetadataEnabled(id, ownerId, enabled)
            ProviderCapability.SEARCH -> installedProviderDao.setSearchEnabled(id, ownerId, enabled)
            ProviderCapability.TRACKER -> installedProviderDao.setTrackerEnabled(id, ownerId, enabled)
        }
    }

    override fun getProviderAsFlow(id: String, ownerId: String): Flow<ProviderResponseWrapper?> {
        return combine(
            installedProviderDao.getAsFlow(id, ownerId),
            pluginsMap.mapLatest { it[id] }.distinctUntilChanged(),
            metadataMap.mapLatest { it[id] }.distinctUntilChanged()
        ) { provider, plugin, metadata ->
            if (provider == null) {
                return@combine null
            }

            ProviderResponseWrapper(
                provider = provider,
                plugin = plugin,
                metadata = metadata,
            )
        }
    }
}
