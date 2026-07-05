package com.flixclusive.data.backup.repository

import android.net.Uri
import com.flixclusive.core.datastore.model.user.BackupOptions
import kotlinx.serialization.Serializable

class NoDataToBackupException : Exception()

@Serializable
data class BackupResult(
    val missingLibraryLists: Set<String> = emptySet(),
    val missingProviders: Set<String> = emptySet(),
    val missingProviderRepositories: Set<String> = emptySet(),
    val missingPreferences: Set<String> = emptySet(),
    val missingSearchHistory: Set<String> = emptySet(),
    val missingWatchProgress: Set<String> = emptySet(),
    val missingCachedLinks: Set<String> = emptySet(),
)

interface BackupRepository {
    suspend fun create(uri: Uri, options: BackupOptions = BackupOptions()): BackupResult

    suspend fun restore(uri: Uri, options: BackupOptions = BackupOptions()): BackupResult
}
