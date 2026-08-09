package com.flixclusive.domain.downloads.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.Formatter
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import java.util.concurrent.atomic.AtomicInteger
import com.flixclusive.core.strings.R as LocaleR

/**
 * Turns a [DownloadItem] into the notification that represents it.
 *
 * Split out of [MediaDownloadService] because none of it needs the service beyond a [Context]:
 * this is pure formatting, while the service is left owning the foreground lifecycle and deciding
 * which notifications should exist at all.
 */
internal class MediaDownloadNotificationFactory(
    private val context: Context,
) {
    /** Item id to the notification id it owns — see [notificationId]. */
    private val notificationIds = mutableMapOf<String, Int>()
    private val nextNotificationId = AtomicInteger(0)

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel() {
        val channel = NotificationChannel(
            MediaDownloadService.NOTIFICATION_CHANNEL_ID,
            context.getString(LocaleR.string.download_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(LocaleR.string.download_notification_channel_desc)
        }

        context.getSystemService<NotificationManager>()!!.createNotificationChannel(channel)
    }

    /**
     * A stable, collision-free notification id per item. `id.hashCode()` was neither: two ids can
     * hash alike and would then share — and overwrite — one notification row. Handing out
     * sequential ids from a map avoids that; the reserved summary/queued-group ids count down from
     * [Int.MAX_VALUE], so counting up from zero can't reach them.
     */
    fun notificationId(item: DownloadItem): Int =
        notificationIds.getOrPut(item.id) { nextNotificationId.getAndIncrement() }

    /**
     * Drops [id]'s reserved notification id and hands it back so the caller can cancel the row.
     *
     * Null when the item never had one. Without this the map would grow by an entry per deleted
     * download for the service's lifetime.
     */
    fun forget(id: String): Int? = notificationIds.remove(id)

    fun buildSummary(activeCount: Int): Notification {
        val text = if (activeCount <= 0) {
            context.getString(LocaleR.string.download_notification_preparing)
        } else {
            context.resources.getQuantityString(
                LocaleR.plurals.download_notification_in_progress,
                activeCount,
                activeCount,
            )
        }

        return buildSimple(
            title = context.getString(LocaleR.string.download_notification_downloading),
            text = text,
            icon = android.R.drawable.stat_sys_download,
            ongoing = true,
            isGroupSummary = true,
        )
    }

    fun buildQueuedGroup(count: Int): Notification = buildSimple(
        title = context.getString(LocaleR.string.download_notification_queued),
        text = context.resources.getQuantityString(
            LocaleR.plurals.download_notification_queued_count,
            count,
            count,
        ),
        icon = android.R.drawable.ic_popup_sync,
        ongoing = true,
    )

    fun buildCompleted(item: DownloadItem): Notification = buildSimple(
        title = item.displayTitle(),
        text = context.getString(LocaleR.string.download_notification_complete),
        icon = android.R.drawable.stat_sys_download_done,
        ongoing = false,
        autoCancel = true,
    )

    fun buildError(item: DownloadItem): Notification {
        val message = item.errorMessage?.takeIf { it.isNotBlank() }
            ?: context.getString(LocaleR.string.download_notification_failed)

        return buildSimple(
            title = item.displayTitle(),
            text = message,
            icon = android.R.drawable.stat_notify_error,
            ongoing = false,
            autoCancel = true,
            bigText = message,
            action = NotificationCompat.Action(
                0,
                context.getString(LocaleR.string.retry),
                actionPendingIntent(MediaDownloadService.ACTION_RETRY, item.id),
            ),
        )
    }

    fun buildItem(item: DownloadItem): Notification {
        val (progress, statusText) = progressAndStatusFor(item)

        val icon = if (item.state == DownloadItemState.PAUSED) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.stat_sys_download
        }

        val builder = NotificationCompat
            .Builder(context, MediaDownloadService.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(item.displayTitle())
            .setContentText(statusText)
            .setSmallIcon(icon)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setOngoing(true)

        // An active transfer with no known total gets an indeterminate bar rather than one pinned
        // at 0% — same reasoning as the download card: bytes are moving, the length just wasn't
        // advertised, and a permanently empty bar reads as a download that never started.
        val isTransferring = item.state == DownloadItemState.DOWNLOADING_STREAM ||
            item.state == DownloadItemState.FETCHING_SUBTITLES
        when {
            progress != null -> builder.setProgress(100, progress, false)
            isTransferring -> builder.setProgress(0, 0, true)
        }

        when (item.state) {
            DownloadItemState.DOWNLOADING_STREAM, DownloadItemState.FETCHING_SUBTITLES ->
                builder.addAction(
                    0,
                    context.getString(LocaleR.string.label_pause),
                    actionPendingIntent(MediaDownloadService.ACTION_PAUSE, item.id),
                )
            DownloadItemState.PAUSED ->
                builder.addAction(
                    0,
                    context.getString(LocaleR.string.label_resume),
                    actionPendingIntent(MediaDownloadService.ACTION_RESUME, item.id),
                )
            else -> Unit
        }

        if (item.state != DownloadItemState.STOPPED) {
            builder.addAction(
                0,
                context.getString(LocaleR.string.label_stop),
                actionPendingIntent(MediaDownloadService.ACTION_STOP, item.id),
            )
        }

        return builder.build()
    }

    private fun buildSimple(
        title: String,
        text: String,
        icon: Int,
        ongoing: Boolean,
        autoCancel: Boolean = false,
        isGroupSummary: Boolean = false,
        bigText: String? = null,
        action: NotificationCompat.Action? = null,
    ): Notification = NotificationCompat
        .Builder(context, MediaDownloadService.NOTIFICATION_CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(text)
        .setSmallIcon(icon)
        .setGroup(NOTIFICATION_GROUP_KEY)
        .setGroupSummary(isGroupSummary)
        .setOngoing(ongoing)
        .setAutoCancel(autoCancel)
        .apply {
            if (bigText != null) setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            if (action != null) addAction(action)
        }.build()

    /** Falls back to the media title, with the `SxxExx` tag appended for episodes — downloads
     * don't persist an episode title of their own (see [DownloadItem]). */
    private fun DownloadItem.displayTitle(): String {
        val season = seasonNumber
        val episode = episodeNumber
        if (season == null || episode == null) return mediaTitle

        val tag = "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
        return context.getString(LocaleR.string.download_meta_separator_format, mediaTitle, tag)
    }

    private fun progressAndStatusFor(item: DownloadItem): Pair<Int?, String> =
        when (item.state) {
            DownloadItemState.DOWNLOADING_STREAM -> {
                val percent = percentOf(item.streamBytesDownloaded, item.streamTotalBytes)
                val speedSuffix = formatSpeedSuffix(item, isSegments = item.isHlsStream)
                val text = when {
                    // HLS reuses these same columns for a segment count, not a byte count — running
                    // it through formatSize() would render a handful of segments as a bogus "1 KB".
                    item.isHlsStream && item.streamTotalBytes > 0 ->
                        context.getString(
                            LocaleR.string.download_progress_hls_format,
                            item.streamBytesDownloaded.toInt(),
                            item.streamTotalBytes.toInt(),
                        )
                    item.streamTotalBytes > 0 ->
                        context.getString(
                            LocaleR.string.download_progress_bytes_format,
                            formatSize(item.streamBytesDownloaded),
                            formatSize(item.streamTotalBytes),
                        )
                    else -> formatSize(item.streamBytesDownloaded)
                }
                percent to text + speedSuffix
            }
            DownloadItemState.FETCHING_SUBTITLES -> {
                val percent = percentOf(item.downloadedSubtitlesCount.toLong(), item.totalSubtitlesCount.toLong())
                // Subtitle transfers are always plain file downloads, regardless of whether the
                // stream itself was HLS — never render this one as "segments/s".
                val speedSuffix = formatSpeedSuffix(item, isSegments = false)
                val text = context.getString(
                    LocaleR.string.download_progress_bytes_format,
                    item.downloadedSubtitlesCount.toString(),
                    item.totalSubtitlesCount.toString(),
                )
                percent to text + speedSuffix
            }
            DownloadItemState.PAUSED -> null to context.getString(LocaleR.string.download_state_paused)
            DownloadItemState.STREAM_COMPLETE ->
                null to context.getString(LocaleR.string.download_notification_preparing_subtitles)
            else -> null to context.getString(LocaleR.string.download_state_queued)
        }

    /** Null when there is no total to be a percentage of — the caller shows an indeterminate bar
     * for that rather than a determinate one stuck at zero. */
    private fun percentOf(
        current: Long,
        total: Long,
    ): Int? = if (total <= 0) null else ((current * 100) / total).toInt().coerceIn(0, 100)

    private fun formatSize(bytes: Long): String = Formatter.formatShortFileSize(context, bytes)

    /** `" • 1.2 MB/s"` (or `" • 3 segments/s"` for HLS). Mirrors the download card: the
     * `"Calculating speed…"` placeholder is only for a download that hasn't moved a byte yet, since
     * once something has transferred a zero rate means the transfer has genuinely stalled — and
     * `" • 0 B/s"` says that far more honestly than a placeholder that never resolves. */
    private fun formatSpeedSuffix(
        item: DownloadItem,
        isSegments: Boolean,
    ): String {
        val hasTransferred = item.streamBytesDownloaded > 0 || item.downloadedSubtitlesCount > 0
        if (item.downloadBytesPerSecond <= 0 && !hasTransferred) {
            return " • ${context.getString(LocaleR.string.download_progress_speed_calculating)}"
        }

        val speed = if (isSegments) {
            context.getString(LocaleR.string.download_progress_speed_segments_format, item.downloadBytesPerSecond)
        } else {
            context.getString(
                LocaleR.string.download_progress_speed_format,
                formatSize(item.downloadBytesPerSecond),
            )
        }

        return " • $speed"
    }

    private fun actionPendingIntent(
        action: String,
        itemId: String,
    ): PendingIntent {
        val intent = Intent(context, MediaDownloadService::class.java).apply {
            this.action = action
            putExtra(MediaDownloadService.EXTRA_ITEM_ID, itemId)
        }

        return PendingIntent.getService(
            context,
            "$action-$itemId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val NOTIFICATION_GROUP_KEY = "media_downloads_group"
    }
}
