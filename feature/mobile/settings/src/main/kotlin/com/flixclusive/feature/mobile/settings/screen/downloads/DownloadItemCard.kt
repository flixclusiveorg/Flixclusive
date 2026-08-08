package com.flixclusive.feature.mobile.settings.screen.downloads

import android.content.Context
import android.text.format.Formatter
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.feature.mobile.settings.util.CacheLinksFormatUtil
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun DownloadItemCard(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDimmed = item.state == DownloadItemState.STOPPED

    // A stopped or failed item isn't going anywhere, so its last progress reading is noise —
    // worse, a half-filled bar reads as though the download were still on its way.
    val hidesProgress = isDimmed || item.state == DownloadItemState.FAILED

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isDimmed) 0.6f else 1f }
            .clip(MaterialTheme.shapes.medium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(12.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.Top) {
                Crossfade(targetState = item.state, label = "DownloadItemStatusIcon") { state ->
                    Icon(
                        painter = painterResource(downloadStateIcon(state)),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp),
                        tint = if (state == DownloadItemState.FAILED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.mediaTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )

                    Text(
                        text = item.subtitleLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    if (item.state == DownloadItemState.COMPLETED) {
                        Text(
                            text = item.completedSummary(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                DownloadItemActions(
                    state = item.state,
                    onPause = onPause,
                    onResume = onResume,
                    onStop = onStop,
                    onRetry = onRetry,
                    onDelete = onDelete,
                    onOpen = onOpen,
                )
            }

            // Shown whenever there's data to report, regardless of the item's current phase —
            // e.g. the stream row stays visible once fetching subtitles starts, not just while
            // DOWNLOADING_STREAM is active, and vice versa. See [hidesProgress] for the states
            // that opt out entirely.
            if (item.streamTotalBytes > 0 && !hidesProgress) {
                Spacer(modifier = Modifier.height(8.dp))
                DownloadProgressRow(
                    label = stringResource(LocaleR.string.download_progress_video_label),
                    progress = { item.streamProgress() },
                    valueText = remember(item) { item.streamValueText(context) },
                )
            }

            if (item.totalSubtitlesCount > 0 && !hidesProgress) {
                Spacer(modifier = Modifier.height(4.dp))
                DownloadProgressRow(
                    label = stringResource(LocaleR.string.download_progress_subtitles_label),
                    progress = { item.subtitlesProgress() },
                    valueText = "${item.downloadedSubtitlesCount} / ${item.totalSubtitlesCount}",
                )
            }

            // Below whichever progress row(s) are showing, and visible for the whole active stretch
            // rather than only while a nonzero rate is available, so it never flickers in and out
            // as fresh rate samples land. STREAM_COMPLETE counts as active for the same reason:
            // it's the brief handover into subtitle fetching, and dropping the row there made the
            // speed blink out mid-download.
            val isActivelyDownloading = item.state == DownloadItemState.DOWNLOADING_STREAM ||
                item.state == DownloadItemState.STREAM_COMPLETE ||
                item.state == DownloadItemState.FETCHING_SUBTITLES

            if (isActivelyDownloading) {
                Text(
                    text = remember(item) { item.downloadSpeedText(context) },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressRow(
    label: String,
    progress: () -> Float,
    valueText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(72.dp),
        )

        LinearProgressIndicator(
            progress = progress,
            drawStopIndicator = {},
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .clip(CircleShape),
        )

        Text(
            text = valueText,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun DownloadItemActions(
    state: DownloadItemState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (state) {
            DownloadItemState.QUEUED -> {
                IconAction(
                    UiCommonR.drawable.round_stop_24,
                    stringResource(LocaleR.string.download_action_stop_content_desc),
                    onStop
                )
            }

            DownloadItemState.DOWNLOADING_STREAM,
            DownloadItemState.STREAM_COMPLETE,
            DownloadItemState.FETCHING_SUBTITLES -> {
                IconAction(
                    UiCommonR.drawable.round_pause_24,
                    stringResource(LocaleR.string.download_action_pause_content_desc),
                    onPause
                )
                IconAction(
                    UiCommonR.drawable.round_stop_24,
                    stringResource(LocaleR.string.download_action_stop_content_desc),
                    onStop
                )
            }

            DownloadItemState.PAUSED -> {
                IconAction(
                    UiCommonR.drawable.play,
                    stringResource(LocaleR.string.download_action_resume_content_desc),
                    onResume
                )
                IconAction(
                    UiCommonR.drawable.round_stop_24,
                    stringResource(LocaleR.string.download_action_stop_content_desc),
                    onStop
                )
            }

            DownloadItemState.COMPLETED -> {
                IconAction(
                    UiCommonR.drawable.play_outline_circle,
                    stringResource(LocaleR.string.download_action_open_content_desc),
                    onOpen
                )
                IconAction(UiCommonR.drawable.delete_outlined, stringResource(LocaleR.string.delete), onDelete)
            }

            DownloadItemState.STOPPED,
            DownloadItemState.FAILED -> {
                IconAction(UiCommonR.drawable.round_refresh_24, stringResource(LocaleR.string.retry), onRetry)
                IconAction(UiCommonR.drawable.delete_outlined, stringResource(LocaleR.string.delete), onDelete)
            }
        }
    }
}

@Composable
private fun IconAction(
    iconId: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun DownloadItem.streamProgress(): Float {
    if (streamTotalBytes <= 0) return 0f
    return (streamBytesDownloaded.toFloat() / streamTotalBytes.toFloat()).coerceIn(0f, 1f)
}

private fun DownloadItem.subtitlesProgress(): Float {
    if (totalSubtitlesCount <= 0) return 0f
    return (downloadedSubtitlesCount.toFloat() / totalSubtitlesCount.toFloat()).coerceIn(0f, 1f)
}

private fun DownloadItem.streamValueText(context: Context): String {
    if (isHlsStream) {
        return context.getString(
            LocaleR.string.download_progress_segments_format,
            streamBytesDownloaded.toInt(),
            streamTotalBytes.toInt(),
        )
    }

    val downloaded = Formatter.formatShortFileSize(context, streamBytesDownloaded)
    val total = Formatter.formatShortFileSize(context, streamTotalBytes)
    return "$downloaded / $total"
}

/** `"1.2 MB/s"` (or `"3 segments/s"` while [DownloadItemState.DOWNLOADING_STREAM] on an HLS
 * stream). The "Calculating…" placeholder is reserved for a download that hasn't moved a byte yet,
 * where there's genuinely no sample to diff against — matching browser download managers. Once
 * something has transferred, a zero rate means the transfer has stalled, and `"0 B/s"` reports that
 * far more honestly than a placeholder that would otherwise never resolve. (Brief stalls don't get
 * that far: the repository replays the last measured rate for a few samples first.) Subtitle
 * transfers are always plain file downloads, so they're never rendered as segments/s even when the
 * video itself was HLS. */
private fun DownloadItem.downloadSpeedText(context: Context): String {
    val hasTransferred = streamBytesDownloaded > 0 || downloadedSubtitlesCount > 0
    if (downloadBytesPerSecond <= 0 && !hasTransferred) {
        return context.getString(LocaleR.string.download_progress_speed_calculating)
    }

    val isSegments = state == DownloadItemState.DOWNLOADING_STREAM && isHlsStream
    return if (isSegments) {
        context.getString(LocaleR.string.download_progress_speed_segments_format, downloadBytesPerSecond)
    } else {
        context.getString(
            LocaleR.string.download_progress_speed_format,
            Formatter.formatShortFileSize(context, downloadBytesPerSecond),
        )
    }
}

@Composable
private fun DownloadItem.completedSummary(): String {
    val context = LocalContext.current
    val size = Formatter.formatShortFileSize(context, streamTotalBytes)

    if (totalSubtitlesCount <= 0) return size

    val subtitleCount = pluralStringResource(
        LocaleR.plurals.download_subtitles_count,
        downloadedSubtitlesCount,
        downloadedSubtitlesCount,
    )
    return "$size • $subtitleCount"
}

@Composable
private fun DownloadItem.subtitleLabel(): String {
    val stateLabel = if (state == DownloadItemState.FAILED && !errorMessage.isNullOrBlank()) {
        errorMessage.orEmpty()
    } else {
        state.label()
    }
    val season = seasonNumber
    val episode = episodeNumber

    if (mediaType == MediaType.SHOW && season != null && episode != null) {
        val episodeTag = CacheLinksFormatUtil.getFormattedTitle(season, episode)
        return "$episodeTag • $stateLabel"
    }

    return stateLabel
}
