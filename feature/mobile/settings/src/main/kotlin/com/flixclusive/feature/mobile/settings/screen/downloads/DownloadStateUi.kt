package com.flixclusive.feature.mobile.settings.screen.downloads

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.drawables.R as UiCommonR

/** How a download entry is identified in a lazy list. */
internal fun DownloadListEntry.key(): String =
    when (this) {
        is DownloadListEntry.Single -> "single-${item.id}"
        is DownloadListEntry.Batch -> "batch-$mediaId-$seasonNumber"
    }

@get:DrawableRes
internal val DownloadItemState.iconRes: Int
    get() = when (this) {
        DownloadItemState.QUEUED -> UiCommonR.drawable.time_circle_outlined
        DownloadItemState.DOWNLOADING_STREAM,
        DownloadItemState.STREAM_COMPLETE,
        DownloadItemState.FETCHING_SUBTITLES,
        -> UiCommonR.drawable.download
        DownloadItemState.PAUSED -> UiCommonR.drawable.time_circle_outlined
        DownloadItemState.COMPLETED -> UiCommonR.drawable.check
        DownloadItemState.STOPPED -> UiCommonR.drawable.outlined_trash
        DownloadItemState.FAILED -> UiCommonR.drawable.round_error_outline_24
    }

@Composable
internal fun DownloadItemState.label(): String =
    when (this) {
        DownloadItemState.QUEUED -> stringResource(LocaleR.string.download_state_queued)
        DownloadItemState.DOWNLOADING_STREAM,
        DownloadItemState.STREAM_COMPLETE,
        -> stringResource(LocaleR.string.download_state_downloading)
        DownloadItemState.FETCHING_SUBTITLES -> stringResource(LocaleR.string.download_state_fetching_subtitles)
        DownloadItemState.PAUSED -> stringResource(LocaleR.string.download_state_paused)
        DownloadItemState.COMPLETED -> stringResource(LocaleR.string.download_state_completed)
        DownloadItemState.STOPPED -> stringResource(LocaleR.string.download_state_stopped)
        DownloadItemState.FAILED -> stringResource(LocaleR.string.download_state_failed)
    }

@Composable
internal fun DownloadStateFilter.label(): String =
    when (this) {
        DownloadStateFilter.QUEUED -> stringResource(LocaleR.string.download_state_queued)
        DownloadStateFilter.DOWNLOADING -> stringResource(LocaleR.string.download_state_downloading)
        DownloadStateFilter.PAUSED -> stringResource(LocaleR.string.download_state_paused)
        DownloadStateFilter.COMPLETED -> stringResource(LocaleR.string.download_state_completed)
        DownloadStateFilter.STOPPED -> stringResource(LocaleR.string.download_state_stopped)
        DownloadStateFilter.FAILED -> stringResource(LocaleR.string.download_state_failed)
    }

@Composable
internal fun MediaType.label(): String =
    when (this) {
        MediaType.MOVIE -> stringResource(LocaleR.string.download_type_movie)
        MediaType.SHOW -> stringResource(LocaleR.string.download_type_show)
    }
