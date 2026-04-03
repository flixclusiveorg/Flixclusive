package com.flixclusive.data.provider.repository.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.provider.InstalledProviderDao
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.util.ProviderSortOrderManager
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import dagger.hilt.android.qualifiers.ApplicationContext
import dalvik.system.PathClassLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ProviderRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient, // TODO: Remove this when we refactor `getApi` to remove the need for it in this class
    private val installedProviderDao: InstalledProviderDao,
    private val appDispatchers: AppDispatchers
) : ProviderRepository {
    private val providerSortOrderManager = ProviderSortOrderManager(installedProviderDao)

    private val metadataMap = HashMap<String, ProviderMetadata>()

    /** Map containing all loaded provider classes  */
    private val pluginsMap: MutableMap<String, Provider> =
        Collections.synchronizedMap(LinkedHashMap())

    // TODO: Make this public for crash log purposes
    private val classLoadersMap: MutableMap<String, PathClassLoader> =
        Collections.synchronizedMap(HashMap())

    override suspend fun load(
        provider: Provider,
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

    override suspend fun uninstall(provider: InstalledProvider)
        = withContext(appDispatchers.io) {
            unload(provider.id)
            installedProviderDao.delete(provider)
        }

    override fun getMetadata(id: String) = metadataMap[id]

    override fun getPlugin(id: String) = pluginsMap[id]

    override suspend fun getInstalledProvider(
        id: String,
        ownerId: Int
    ): InstalledProvider? {
        return withContext(appDispatchers.io) {
            installedProviderDao.get(id, ownerId)
        }
    }

    override suspend fun getApi(id: String, ownerId: Int): ProviderApi? = withContext(appDispatchers.io) {
        val provider = pluginsMap[id] ?: return@withContext null
        try {
            provider.getApi(context, okHttpClient)
        } catch (e: Exception) {
            warnLog("API for provider [$id] failed to instantiate. This provider will be disabled.")
            errorLog(e)
            installedProviderDao.setEnabled(
                id = id,
                ownerId = ownerId,
                isEnabled = false
            )
            null
        }
    }

    override suspend fun isEnabled(id: String, ownerId: Int)
        = withContext(appDispatchers.io) {
            installedProviderDao.isEnabled(id = id, ownerId = ownerId)
        }

    override fun getEnabledProvidersAsFlow(ownerId: Int): Flow<List<InstalledProvider>> {
        return installedProviderDao.getEnabledAsFlow(ownerId)
    }

    override suspend fun getEnabledProviders(ownerId: Int): List<InstalledProvider> {
        return withContext(appDispatchers.io) {
            installedProviderDao.getEnabled(ownerId)
        }
    }

    override suspend fun getInstalledProviders(ownerId: Int)
        = withContext(appDispatchers.io) {
            installedProviderDao.getAll(ownerId)
        }

    override fun getInstalledProvidersAsFlow(ownerId: Int) = installedProviderDao.getAllAsFlow(ownerId)

    override suspend fun getMaxSortOrder(ownerId: Int): Double {
        return withContext(appDispatchers.io) {
            providerSortOrderManager.getNextSortOrder(ownerId)
        }
    }

    override suspend fun moveProvider(
        from: Int,
        to: Int,
        ownerId: Int
    ) {
        providerSortOrderManager.reorder(
            ownerId = ownerId,
            from = from,
            to = to
        )
    }

    override suspend fun clearAll() {
        pluginsMap.clear()
        classLoadersMap.clear()
        metadataMap.clear()
    }

    override suspend fun toggleProvider(id: String, ownerId: Int) {
        val isEnabled = installedProviderDao.isEnabled(id, ownerId)
        installedProviderDao.setEnabled(
            id = id,
            ownerId = ownerId,
            isEnabled = !isEnabled
        )
    }
}
