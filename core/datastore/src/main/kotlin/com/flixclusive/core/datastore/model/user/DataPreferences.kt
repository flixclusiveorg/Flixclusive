package com.flixclusive.core.datastore.model.user

import kotlinx.serialization.Serializable

@Serializable
data class DataPreferences(
    val isIncognito: Boolean = false,

    /**
     * Auto-backup frequency in days.
     *
     * - `0` means OFF.
     * - Values are clamped by the scheduler/worker.
     */
    val autoBackupFrequencyDays: Int = DEFAULT_AUTO_BACKUP_FREQUENCY_DAYS,

    /**
     * Max backups to keep per user. When exceeded, the oldest backup is replaced.
     */
    val maxBackups: Int = DEFAULT_MAX_BACKUPS,

    /**
     * Which parts to include for auto-backups.
     */
    val autoBackupOptions: BackupOptions = BackupOptions(),
) : UserPreferences

private const val DEFAULT_AUTO_BACKUP_FREQUENCY_DAYS = 7
private const val DEFAULT_MAX_BACKUPS = 5
