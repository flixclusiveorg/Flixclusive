package com.flixclusive.core.database.entity.downloads

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flixclusive.model.media.common.MediaType
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "download_items",
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["state"]),
        Index(value = ["ownerId"]),
    ],
)
data class DownloadItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val mediaId: String,
    val mediaTitle: String,
    val mediaType: MediaType,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val state: DownloadItemState = DownloadItemState.QUEUED,
    val phase: DownloadPhase? = null,
    /** Foreign key into `cached_streams.url` — the link this item is currently downloading (or
     * last downloaded from). Persisted rather than re-resolved so a resume after process death
     * knows which link produced the partial file already on disk. */
    val sourceUrl: String? = null,
    /** Whether [sourceUrl] is an HLS manifest rather than a direct file — determines which
     * transfer engine downloads it and how [streamBytesDownloaded]/[streamTotalBytes] are
     * interpreted (segments, not bytes, for HLS). Only ever describes the video stream —
     * [downloadBytesPerSecond] stays byte-based for subtitles regardless of this flag. Rewritten
     * every time [sourceUrl] changes so the two can never disagree. */
    val isHlsStream: Boolean = false,
    /** SAF document URI (resolved via [com.hippo.unifile.UniFile]) of the downloaded video file
     * on disk. Sibling subtitle files live in that file's parent's `subtitles/` folder. */
    val streamFilePath: String? = null,
    val streamBytesDownloaded: Long = 0,
    val streamTotalBytes: Long = 0,
    /** Transfer rate since the previous throttled progress write, for whichever transfer is
     * currently active — the video stream (bytes/sec, or segments/sec if [isHlsStream]) or a
     * single subtitle file (always bytes/sec). Only meaningful while [state] is
     * [DownloadItemState.DOWNLOADING_STREAM] or [DownloadItemState.FETCHING_SUBTITLES]; not reset
     * when either phase ends, so it's the caller's responsibility not to display a stale value
     * once nothing is actively transferring. */
    val downloadBytesPerSecond: Long = 0,
    val downloadedSubtitlesCount: Int = 0,
    val totalSubtitlesCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
)

/**
 * A single 0f–1f fraction combining the video stream and subtitle transfers into one number —
 * e.g. for a determinate progress ring where there's no room to show the two separately.
 *
 * The stream is weighted to [STREAM_PROGRESS_WEIGHT] of the total and subtitles the remainder,
 * fixed regardless of whether [DownloadItem.totalSubtitlesCount] is even known yet — it's only
 * populated once [DownloadItemState.FETCHING_SUBTITLES] actually starts (see
 * `MediaDownloadControllerImpl.runSubtitlePhase`). Weighting on state rather than raw byte/count
 * ratios keeps the value monotonically increasing: a naive average of the two ratios would jump
 * to 100% the instant the stream finishes (subtitle total still reads as 0/0) and then drop back
 * down once the real subtitle count arrives.
 */
fun DownloadItem.combinedProgress(): Float {
    if (state == DownloadItemState.COMPLETED) return 1f

    val streamFraction = if (streamTotalBytes > 0) {
        (streamBytesDownloaded.toFloat() / streamTotalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    if (state != DownloadItemState.FETCHING_SUBTITLES && state != DownloadItemState.STREAM_COMPLETE) {
        return streamFraction * STREAM_PROGRESS_WEIGHT
    }

    val subtitleFraction = if (totalSubtitlesCount > 0) {
        (downloadedSubtitlesCount.toFloat() / totalSubtitlesCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    return STREAM_PROGRESS_WEIGHT + subtitleFraction * (1f - STREAM_PROGRESS_WEIGHT)
}

private const val STREAM_PROGRESS_WEIGHT = 0.9f
