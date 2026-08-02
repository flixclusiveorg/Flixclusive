package com.flixclusive.feature.mobile.settings.screen.downloads

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.feature.mobile.settings.screen.links.util.CacheLinksFormatUtil
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
    val isDimmed = item.state == DownloadItemState.STOPPED
    val progress = item.progress()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isDimmed) 0.6f else 1f)
            .clip(MaterialTheme.shapes.medium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    painter = painterResource(downloadStateIcon(item.state)),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp),
                    tint = if (item.state == DownloadItemState.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )

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

            if (item.state.isTransferring() && progress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape),
                )
            } else if (item.state == DownloadItemState.PAUSED && progress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
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

private fun DownloadItemState.isTransferring(): Boolean = this == DownloadItemState.DOWNLOADING_STREAM ||
    this == DownloadItemState.STREAM_COMPLETE ||
    this == DownloadItemState.FETCHING_SUBTITLES

private fun DownloadItem.progress(): Float? {
    val (downloaded, total) = if (state == DownloadItemState.FETCHING_SUBTITLES) {
        subtitleBytesDownloaded to subtitleTotalBytes
    } else {
        streamBytesDownloaded to streamTotalBytes
    }

    if (total <= 0) return null
    return (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

@Composable
private fun DownloadItem.subtitleLabel(): String {
    val stateLabel = state.label()
    val season = seasonNumber
    val episode = episodeNumber

    if (mediaType == MediaType.SHOW && season != null && episode != null) {
        val episodeTag = CacheLinksFormatUtil.getFormattedTitle(season, episode)
        return "$episodeTag • $stateLabel"
    }

    return stateLabel
}
