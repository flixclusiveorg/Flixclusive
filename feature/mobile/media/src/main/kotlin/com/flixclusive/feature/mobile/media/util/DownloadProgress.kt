package com.flixclusive.feature.mobile.media.util

import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState

/**
 * A single 0f–1f fraction combining the video stream and subtitle transfers into one number —
 * e.g. for a determinate progress ring where there's no room to show the two separately.
 *
 * The stream is weighted to [STREAM_PROGRESS_WEIGHT] of the total and subtitles the remainder,
 * fixed regardless of whether [DownloadItem.totalSubtitlesCount] is even known yet — it's only
 * populated once [DownloadItemState.FETCHING_SUBTITLES] actually starts. Weighting on state rather
 * than raw byte/count ratios keeps the value monotonically increasing: a naive average of the two
 * ratios would jump to 100% the instant the stream finishes (subtitle total still reads as 0/0) and
 * then drop back down once the real subtitle count arrives.
 */
internal fun DownloadItem.combinedProgress(): Float {
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
