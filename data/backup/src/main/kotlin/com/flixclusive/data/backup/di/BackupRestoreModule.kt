package com.flixclusive.data.backup.di

import com.flixclusive.data.backup.model.BackupLibraryList
import com.flixclusive.data.backup.model.BackupPreference
import com.flixclusive.data.backup.model.BackupProvider
import com.flixclusive.data.backup.model.BackupProviderRepository
import com.flixclusive.data.backup.model.BackupSearchHistory
import com.flixclusive.data.backup.model.BackupWatchProgress
import com.flixclusive.data.backup.restore.BackupRestorer
import com.flixclusive.data.backup.restore.impl.LibraryListBackupRestorer
import com.flixclusive.data.backup.restore.impl.PreferenceBackupRestorer
import com.flixclusive.data.backup.restore.impl.ProviderBackupRestorer
import com.flixclusive.data.backup.restore.impl.RepositoryBackupRestorer
import com.flixclusive.data.backup.restore.impl.SearchHistoryBackupRestorer
import com.flixclusive.data.backup.restore.impl.WatchProgressBackupRestorer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupRestoreModule {
    @Binds
    @Singleton
    abstract fun bindsLibraryListBackupRestorer(restorer: LibraryListBackupRestorer): BackupRestorer<BackupLibraryList>

    @Binds
    @Singleton
    abstract fun bindsPreferenceBackupRestorer(restorer: PreferenceBackupRestorer): BackupRestorer<BackupPreference>

    @Binds
    @Singleton
    abstract fun bindsWatchProgressBackupRestorer(restorer: WatchProgressBackupRestorer): BackupRestorer<BackupWatchProgress>

    @Binds
    @Singleton
    abstract fun bindsSearchHistoryBackupRestorer(restorer: SearchHistoryBackupRestorer): BackupRestorer<BackupSearchHistory>

    @Binds
    @Singleton
    abstract fun bindsProviderBackupRestorer(restorer: ProviderBackupRestorer): BackupRestorer<BackupProvider>

    @Binds
    @Singleton
    abstract fun bindsRepositoryBackupRestorer(restorer: RepositoryBackupRestorer): BackupRestorer<BackupProviderRepository>
}
