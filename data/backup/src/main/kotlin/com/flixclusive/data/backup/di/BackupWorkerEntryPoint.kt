package com.flixclusive.data.backup.di

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.UserDao
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.repository.BackupRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface BackupWorkerEntryPoint {
    fun backupRepository(): BackupRepository
    fun appDispatchers(): AppDispatchers
    fun userSessionDataStore(): UserSessionDataStore
    fun dataStoreManager(): DataStoreManager
    fun userDao(): UserDao
}
