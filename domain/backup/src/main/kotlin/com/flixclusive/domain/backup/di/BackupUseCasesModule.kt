package com.flixclusive.domain.backup.di

import com.flixclusive.domain.backup.usecase.CreateBackupUseCase
import com.flixclusive.domain.backup.usecase.RestoreBackupUseCase
import com.flixclusive.domain.backup.usecase.impl.CreateBackupUseCaseImpl
import com.flixclusive.domain.backup.usecase.impl.RestoreBackupUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupUseCasesModule {
    @Binds
    abstract fun bindCreateBackupUseCase(impl: CreateBackupUseCaseImpl): CreateBackupUseCase

    @Binds
    abstract fun bindRestoreBackupUseCase(impl: RestoreBackupUseCaseImpl): RestoreBackupUseCase
}
