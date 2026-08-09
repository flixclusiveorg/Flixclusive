package com.flixclusive.core.datastore.model.user

import com.flixclusive.core.datastore.model.user.download.DownloadLinkSelectionMode
import com.flixclusive.core.datastore.model.user.download.DownloadLinkSortDirection
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
     * Preference for ranking candidate streaming links when starting a download: closest to the
     * player's preferred quality first, or by known size first. Speed is always the secondary
     * sort key. See [downloadLinkSortDirection] for which end of that axis to prefer.
     */
    val downloadLinkSelectionMode: DownloadLinkSelectionMode = DownloadLinkSelectionMode.QUALITY_FIRST,
    /**
     * Which end of [downloadLinkSelectionMode]'s axis to prefer — see [DownloadLinkSortDirection].
     */
    val downloadLinkSortDirection: DownloadLinkSortDirection = DownloadLinkSortDirection.HIGHEST_FIRST,
    /**
     * Maximum number of downloads (movies/episodes) allowed to transfer at the same time.
     * Items beyond this limit stay queued (FIFO by queue time) until a slot frees up.
     */
    val downloadConcurrencyLimit: Int = DEFAULT_DOWNLOAD_CONCURRENCY_LIMIT,
    /**
     * Whether downloads may only run on an unmetered connection. Governs every path that starts a
     * transfer, an explicit tap included — the setting promises downloads happen on Wi-Fi, not that
     * they merely resume there. A download asked for on mobile data is still queued and still
     * listed; it simply waits, and begins on its own once an unmetered connection is back.
     *
     * Defaults to on, since the alternative default spends someone's data allowance without asking.
     */
    val downloadOnWifiOnly: Boolean = true,
) : UserPreferences

private const val DEFAULT_AUTO_BACKUP_FREQUENCY_DAYS = 7
private const val DEFAULT_MAX_BACKUPS = 5
private const val DEFAULT_DEAD_LINK_RETENTION_DAYS = 1
private const val DEFAULT_DOWNLOAD_CONCURRENCY_LIMIT = 3
