package com.flixclusive.data.backup.di

import com.flixclusive.data.backup.create.BackupCreator
import com.flixclusive.data.backup.create.impl.LibraryListBackupCreator
import com.flixclusive.data.backup.create.impl.PreferenceBackupCreator
import com.flixclusive.data.backup.create.impl.ProviderBackupCreator
import com.flixclusive.data.backup.create.impl.RepositoryBackupCreator
import com.flixclusive.data.backup.create.impl.SearchHistoryBackupCreator
import com.flixclusive.data.backup.create.impl.WatchProgressBackupCreator
import com.flixclusive.data.backup.model.BackupLibraryList
import com.flixclusive.data.backup.model.BackupPreference
import com.flixclusive.data.backup.model.BackupProvider
import com.flixclusive.data.backup.model.BackupProviderRepository
import com.flixclusive.data.backup.model.BackupSearchHistory
import com.flixclusive.data.backup.model.BackupWatchProgress
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupCreateModule {
    @Binds
    @Singleton
    abstract fun bindsLibraryListBackupCreator(creator: LibraryListBackupCreator): BackupCreator<BackupLibraryList>

    @Binds
    @Singleton
    abstract fun bindsPreferenceBackupCreator(creator: PreferenceBackupCreator): BackupCreator<BackupPreference>

    @Binds
    @Singleton
    abstract fun bindsWatchProgressBackupCreator(creator: WatchProgressBackupCreator): BackupCreator<BackupWatchProgress>

    @Binds
    @Singleton
    abstract fun bindsSearchHistoryBackupCreator(creator: SearchHistoryBackupCreator): BackupCreator<BackupSearchHistory>

    @Binds
    @Singleton
    abstract fun bindsProviderBackupCreator(creator: ProviderBackupCreator): BackupCreator<BackupProvider>

    @Binds
    @Singleton
    abstract fun bindsRepositoryBackupCreator(creator: RepositoryBackupCreator): BackupCreator<BackupProviderRepository>
}
