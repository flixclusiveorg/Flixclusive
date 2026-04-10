package com.flixclusive.data.backup.repository

import android.net.Uri
import com.flixclusive.data.backup.model.BackupOptions

class NoDataToBackupException : Exception()

data class BackupResult(
    val missingLibraryLists: Set<String>,
    val missingProviders: Set<String>,
    val missingProviderRepositories: Set<String>,
    val missingPreferences: Set<String>,
    val missingSearchHistory: Set<String>,
    val missingWatchProgress: Set<String>,
)

interface BackupRepository {
    suspend fun create(uri: Uri, options: BackupOptions = BackupOptions()): BackupResult

    suspend fun restore(uri: Uri, options: BackupOptions = BackupOptions()): BackupResult
}
