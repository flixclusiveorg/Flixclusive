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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject

internal class ProviderRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val installedProviderDao: InstalledProviderDao,
    private val appDispatchers: AppDispatchers
) : ProviderRepository {
    private val metadataMap = HashMap<String, ProviderMetadata>()

    /** Map containing all loaded provider classes  */
    private val pluginsMap: MutableMap<String, ProviderPlugin> =
        Collections.synchronizedMap(LinkedHashMap())

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

    override suspend fun unload(id: String) {
        withContext(appDispatchers.io) {
            pluginsMap[id]?.onUnload(context)
        }

        metadataMap.remove(id)
        classLoadersMap.remove(id)
        pluginsMap.remove(id)
    }

    override suspend fun install(
        provider: InstalledProvider,
        metadata: ProviderMetadata
    ) = withContext(appDispatchers.io) {
        installedProviderDao.insert(provider)
        metadataMap[provider.id] = metadata
    }

    override suspend fun uninstall(provider: InstalledProvider) = withContext(appDispatchers.io) {
        unload(provider.id)
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

    override fun getProvidersAsFlow(ownerId: String) = installedProviderDao.getAllAsFlow(ownerId).mapLatest {
        it.map { provider ->
            val plugin = pluginsMap[provider.id]
            val metadata = metadataMap[provider.id]

            ProviderResponseWrapper(
                provider = provider,
                plugin = plugin,
                metadata = metadata
            )
        }
    }

    override fun getProvidersWithCapabilityAsFlow(ownerId: String, capability: ProviderCapability): Flow<List<ProviderResponseWrapper>> {
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

    override suspend fun getProvidersWithCapability(ownerId: String, capability: ProviderCapability): List<ProviderResponseWrapper> {
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
        classLoadersMap.clear()
        metadataMap.clear()
    }

    override suspend fun toggleCapability(id: String, ownerId: String, capability: ProviderCapability) {
        val provider = installedProviderDao.get(id, ownerId) ?: return
        when (capability) {
            ProviderCapability.CATALOG -> installedProviderDao.setCatalogEnabled(id, ownerId, !provider.isCatalogEnabled)
            ProviderCapability.CROSS_MATCH -> installedProviderDao.setCrossMatchEnabled(id, ownerId, !provider.isCrossMatchEnabled)
            ProviderCapability.MEDIA_LINK -> installedProviderDao.setMediaLinkEnabled(id, ownerId, !provider.isMediaLinkEnabled)
            ProviderCapability.METADATA -> installedProviderDao.setMetadataEnabled(id, ownerId, !provider.isMetadataEnabled)
            ProviderCapability.SEARCH -> installedProviderDao.setSearchEnabled(id, ownerId, !provider.isSearchEnabled)
            ProviderCapability.TRACKER -> installedProviderDao.setTrackerEnabled(id, ownerId, !provider.isTrackerEnabled)
        }
    }

    override fun getProviderAsFlow(id: String, ownerId: String): Flow<ProviderResponseWrapper?> {
        return installedProviderDao.getAsFlow(id, ownerId).mapLatest { provider ->
            if (provider == null) return@mapLatest null
            ProviderResponseWrapper(
                provider = provider,
                plugin = pluginsMap[id],
                metadata = metadataMap[id],
            )
        }
    }
}
