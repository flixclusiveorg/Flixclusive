package com.flixclusive.core.datastore.model.user

import com.flixclusive.core.datastore.model.user.download.DownloadLinkSelectionMode
import kotlinx.serialization.Serializable

@Serializable
data class DataPreferences(
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
    /**
     * How many days a dead (failed) cached stream is kept before the cleanup worker removes it.
     *
     * - `0` means dead streams are removed immediately on the next cleanup run.
     */
    val deadLinkRetentionDays: Int = DEFAULT_DEAD_LINK_RETENTION_DAYS,
    /**
     * Preference for ranking candidate streaming links when starting a download:
     * closest to the player's preferred quality first, or largest known size first.
     * Speed is always the secondary sort key.
     */
    val downloadLinkSelectionMode: DownloadLinkSelectionMode = DownloadLinkSelectionMode.QUALITY_FIRST,
) : UserPreferences

private const val DEFAULT_AUTO_BACKUP_FREQUENCY_DAYS = 7
private const val DEFAULT_MAX_BACKUPS = 5
private const val DEFAULT_DEAD_LINK_RETENTION_DAYS = 1
