package com.flixclusive.data.backup.di

import com.flixclusive.data.backup.repository.BackupRepository
import com.flixclusive.data.backup.repository.impl.BackupRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupModule {
    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
