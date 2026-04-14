package com.flixclusive.data.backup.di

import com.flixclusive.data.backup.model.BackupLibraryList
import com.flixclusive.data.backup.model.BackupPreference
import com.flixclusive.data.backup.model.BackupProvider
import com.flixclusive.data.backup.model.BackupProviderRepository
import com.flixclusive.data.backup.model.BackupSearchHistory
import com.flixclusive.data.backup.model.BackupWatchProgress
import com.flixclusive.data.backup.validate.BackupValidator
import com.flixclusive.data.backup.validate.impl.LibraryListBackupValidator
import com.flixclusive.data.backup.validate.impl.PreferenceBackupValidator
import com.flixclusive.data.backup.validate.impl.ProviderBackupValidator
import com.flixclusive.data.backup.validate.impl.RepositoryBackupValidator
import com.flixclusive.data.backup.validate.impl.SearchHistoryBackupValidator
import com.flixclusive.data.backup.validate.impl.WatchProgressBackupValidator
import dagger.Module
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ValidateModule {
    @Binds
    @Singleton
    abstract fun bindLibraryListBackupValidator(
        impl: LibraryListBackupValidator,
    ): BackupValidator<BackupLibraryList>

    @Binds
    @Singleton
    abstract fun bindPreferenceBackupValidator(
        impl: PreferenceBackupValidator,
    ): BackupValidator<BackupPreference>

    @Binds
    @Singleton
    abstract fun bindProviderBackupValidator(
        impl: ProviderBackupValidator,
    ): BackupValidator<BackupProvider>

    @Binds
    @Singleton
    abstract fun bindRepositoryBackupValidator(
        impl: RepositoryBackupValidator,
    ): BackupValidator<BackupProviderRepository>

    @Binds
    @Singleton
    abstract fun bindSearchHistoryBackupValidator(
        impl: SearchHistoryBackupValidator,
    ): BackupValidator<BackupSearchHistory>

    @Binds
    @Singleton
    abstract fun bindWatchProgressBackupValidator(
        impl: WatchProgressBackupValidator,
    ): BackupValidator<BackupWatchProgress>
}
