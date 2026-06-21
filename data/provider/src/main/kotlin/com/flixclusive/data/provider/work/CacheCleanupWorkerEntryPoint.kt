package com.flixclusive.data.provider.work

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.provider.CachedMediaLinkDao
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface CacheCleanupWorkerEntryPoint {
    fun appDispatchers(): AppDispatchers

    fun userSessionDataStore(): UserSessionDataStore

    fun dataStoreManager(): DataStoreManager

    fun cachedMediaLinkDao(): CachedMediaLinkDao
}
