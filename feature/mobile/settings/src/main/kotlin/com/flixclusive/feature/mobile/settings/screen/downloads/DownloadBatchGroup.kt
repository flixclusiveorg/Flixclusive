package com.flixclusive.feature.mobile.settings.screen.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.feature.mobile.settings.component.SettingsListCard
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun DownloadBatchGroup(
    entry: DownloadListEntry.Batch,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPauseBatch: () -> Unit,
    onStopBatch: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onStop: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keyed on the item list rather than derived: these scan every episode, so they should be
    // recomputed when that list actually changes and not on each unrelated recomposition.
    val canPauseBatch = remember(entry.items) {
        entry.items.any { !it.state.isTerminal && it.state != DownloadItemState.PAUSED }
    }
    val canStopBatch = remember(entry.items) { entry.items.any { !it.state.isTerminal } }
    val itemsSize = entry.items.size

    Column(modifier = modifier) {
        SettingsListCard(onClick = onToggleExpand) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.mediaTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )

                    Text(
                        text = stringResource(
                            LocaleR.string.label_format_untitled_season,
                            entry.seasonNumber
                        ) +
                            " • " +
                            pluralStringResource(
                                LocaleR.plurals.download_batch_episode_count,
                                itemsSize,
                                itemsSize
                            ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                if (canPauseBatch) {
                    IconAction(
                        iconId = UiCommonR.drawable.round_pause_24,
                        contentDescription = stringResource(LocaleR.string.download_action_pause_content_desc),
                        onClick = onPauseBatch,
                    )
                }

                if (canStopBatch) {
                    IconAction(
                        iconId = UiCommonR.drawable.round_stop_24,
                        contentDescription = stringResource(LocaleR.string.download_action_stop_content_desc),
                        onClick = onStopBatch,
                    )
                }

                IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(
                            if (isExpanded) UiCommonR.drawable.up_arrow else UiCommonR.drawable.down_arrow
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = LocalContentColor.current.copy(alpha = 0.6f)
                    )
                }
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .padding(start = 20.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                entry.items.forEach { item ->
                    DownloadItemCard(
                        item = item,
                        onPause = { onPause(item.id) },
                        onResume = { onResume(item.id) },
                        onStop = { onStop(item.id) },
                        onRetry = { onRetry(item.id) },
                        onDelete = { onDelete(item.id) },
                        onOpen = { onOpen(item) },
                    )
                }
            }
        }
    }
}
