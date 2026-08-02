package com.flixclusive.feature.mobile.settings.screen.downloads

import com.flixclusive.core.database.entity.downloads.DownloadItemState

/**
 * User-facing state buckets (per spec: queued/downloading/paused/completed/stopped/failed).
 * [DownloadItemState.STREAM_COMPLETE] and [DownloadItemState.FETCHING_SUBTITLES] are internal
 * sub-phases of an in-progress download, so they fold into [DOWNLOADING] rather than getting
 * their own filter chip.
 */
internal enum class DownloadStateFilter(
    val states: Set<DownloadItemState>,
) {
    QUEUED(setOf(DownloadItemState.QUEUED)),
    DOWNLOADING(
        setOf(
            DownloadItemState.DOWNLOADING_STREAM,
            DownloadItemState.STREAM_COMPLETE,
            DownloadItemState.FETCHING_SUBTITLES,
        )
    ),
    PAUSED(setOf(DownloadItemState.PAUSED)),
    COMPLETED(setOf(DownloadItemState.COMPLETED)),
    STOPPED(setOf(DownloadItemState.STOPPED)),
    FAILED(setOf(DownloadItemState.FAILED)),
}
