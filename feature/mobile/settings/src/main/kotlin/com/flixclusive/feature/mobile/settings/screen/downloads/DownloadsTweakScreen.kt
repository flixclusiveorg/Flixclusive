package com.flixclusive.feature.mobile.settings.screen.downloads

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.material3.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBar
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.domain.downloads.usecase.CompletedDownloadFile
import com.flixclusive.model.media.common.MediaType
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.coroutines.flow.collectLatest
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

interface NavigatorDownloadsTweakScreen : NavigateBack

@Destination<ExternalModuleGraph>
@Composable
internal fun DownloadsTweakScreen(
    navigator: NavigatorDownloadsTweakScreen,
    viewModel: DownloadsTweakViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val stateFilters by viewModel.stateFilters.collectAsStateWithLifecycle()
    val typeFilters by viewModel.typeFilters.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is DownloadsTweakEvent.OpenFile -> openDownloadedFile(context, event.file)
            }
        }
    }

    DownloadsTweakScreenContent(
        entries = entries,
        query = query,
        stateFilters = stateFilters,
        typeFilters = typeFilters,
        onNavigateBack = navigator::navigateBack,
        onQueryChange = viewModel::onQueryChange,
        onToggleStateFilter = viewModel::onToggleStateFilter,
        onToggleTypeFilter = viewModel::onToggleTypeFilter,
        onPause = viewModel::onPause,
        onResume = viewModel::onResume,
        onStop = viewModel::onStop,
        onRetry = viewModel::onRetry,
        onDelete = viewModel::onDelete,
        onOpen = viewModel::onOpen,
        onPauseBatch = viewModel::onPauseBatch,
        onStopBatch = viewModel::onStopBatch,
    )
}

private fun openDownloadedFile(context: Context, file: CompletedDownloadFile) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(file.uri, file.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsTweakScreenContent(
    entries: Async<List<DownloadListEntry>>,
    query: String,
    stateFilters: Set<DownloadStateFilter>,
    typeFilters: Set<MediaType>,
    onNavigateBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleStateFilter: (DownloadStateFilter) -> Unit,
    onToggleTypeFilter: (MediaType) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onStop: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onPauseBatch: (String, Int) -> Unit,
    onStopBatch: (String, Int) -> Unit,
) {
    var expandedBatches by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        contentWindowInsets = WindowInsets(),
        topBar = {
            CommonTopBar(
                navigationIcon = {
                    ActionButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.left_arrow),
                            contentDescription = null
                        )
                    }
                },
                title = { Text(text = stringResource(LocaleR.string.downloads)) },
            )
        },
        modifier = Modifier.padding(LocalGlobalScaffoldPadding.current)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(text = stringResource(LocaleR.string.download_search_placeholder)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(UiCommonR.drawable.search_outlined),
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(DownloadStateFilter.entries) { filter ->
                    val isSelected = filter in stateFilters
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleStateFilter(filter) },
                        label = { Text(filter.label()) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected)
                    )
                }

                items(MediaType.entries) { type ->
                    val isSelected = type in typeFilters
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleTypeFilter(type) },
                        label = { Text(type.label()) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            AsyncAnimatedContent(
                targetState = entries,
                modifier = Modifier.fillMaxSize(),
                loadingContent = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                errorContent = {
                    RetryButton(
                        modifier = Modifier.fillMaxSize(),
                        error = it.message.asString(),
                        // The list is a hot, self-recovering DB flow (no one-shot load to redo).
                        onRetry = {},
                    )
                },
            ) { entriesProvider ->
                if (entriesProvider().isEmpty()) {
                    EmptyDataMessage(
                        modifier = Modifier.fillMaxSize(),
                        emojiHeader = "📥",
                        title = stringResource(LocaleR.string.download_empty_title),
                        description = stringResource(LocaleR.string.download_empty_description),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(entriesProvider(), key = { it.key() }) { entry ->
                            when (entry) {
                                is DownloadListEntry.Single -> DownloadItemCard(
                                    item = entry.item,
                                    onPause = { onPause(entry.item.id) },
                                    onResume = { onResume(entry.item.id) },
                                    onStop = { onStop(entry.item.id) },
                                    onRetry = { onRetry(entry.item.id) },
                                    onDelete = { onDelete(entry.item.id) },
                                    onOpen = { onOpen(entry.item) },
                                    modifier = Modifier.animateItem()
                                )

                                is DownloadListEntry.Batch -> DownloadBatchGroup(
                                    entry = entry,
                                    isExpanded = entry.batchKey() in expandedBatches,
                                    onToggleExpand = {
                                        expandedBatches = expandedBatches.toggleBatch(entry.batchKey())
                                    },
                                    onPauseBatch = { onPauseBatch(entry.mediaId, entry.seasonNumber) },
                                    onStopBatch = { onStopBatch(entry.mediaId, entry.seasonNumber) },
                                    onPause = onPause,
                                    onResume = onResume,
                                    onStop = onStop,
                                    onRetry = onRetry,
                                    onDelete = onDelete,
                                    onOpen = onOpen,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Set<String>.toggleBatch(key: String): Set<String> = if (key in this) this - key else this + key

private fun DownloadListEntry.key(): String = when (this) {
    is DownloadListEntry.Single -> "single-${item.id}"
    is DownloadListEntry.Batch -> "batch-${batchKey()}"
}

private fun DownloadListEntry.Batch.batchKey(): String = "$mediaId-$seasonNumber"

@Composable
private fun DownloadStateFilter.label(): String = when (this) {
    DownloadStateFilter.QUEUED -> stringResource(LocaleR.string.download_state_queued)
    DownloadStateFilter.DOWNLOADING -> stringResource(LocaleR.string.download_state_downloading)
    DownloadStateFilter.PAUSED -> stringResource(LocaleR.string.download_state_paused)
    DownloadStateFilter.COMPLETED -> stringResource(LocaleR.string.download_state_completed)
    DownloadStateFilter.STOPPED -> stringResource(LocaleR.string.download_state_stopped)
    DownloadStateFilter.FAILED -> stringResource(LocaleR.string.download_state_failed)
}

@Composable
private fun MediaType.label(): String = when (this) {
    MediaType.MOVIE -> stringResource(LocaleR.string.download_type_movie)
    MediaType.SHOW -> stringResource(LocaleR.string.download_type_show)
}

@Composable
internal fun downloadStateIcon(state: DownloadItemState): Int = when (state) {
    DownloadItemState.QUEUED -> UiCommonR.drawable.time_circle_outlined
    DownloadItemState.DOWNLOADING_STREAM,
    DownloadItemState.STREAM_COMPLETE,
    DownloadItemState.FETCHING_SUBTITLES -> UiCommonR.drawable.download
    DownloadItemState.PAUSED -> UiCommonR.drawable.time_circle_outlined
    DownloadItemState.COMPLETED -> UiCommonR.drawable.check
    DownloadItemState.STOPPED -> UiCommonR.drawable.outlined_trash
    DownloadItemState.FAILED -> UiCommonR.drawable.round_error_outline_24
}

@Composable
internal fun DownloadItemState.label(): String = when (this) {
    DownloadItemState.QUEUED -> stringResource(LocaleR.string.download_state_queued)
    DownloadItemState.DOWNLOADING_STREAM,
    DownloadItemState.STREAM_COMPLETE -> stringResource(LocaleR.string.download_state_downloading)
    DownloadItemState.FETCHING_SUBTITLES -> stringResource(LocaleR.string.download_state_fetching_subtitles)
    DownloadItemState.PAUSED -> stringResource(LocaleR.string.download_state_paused)
    DownloadItemState.COMPLETED -> stringResource(LocaleR.string.download_state_completed)
    DownloadItemState.STOPPED -> stringResource(LocaleR.string.download_state_stopped)
    DownloadItemState.FAILED -> stringResource(LocaleR.string.download_state_failed)
}

@Preview
@Composable
private fun DownloadsTweakScreenPreview() {
    val items = List(3) { i ->
        DownloadItem(
            id = i.toLong(),
            mediaId = "media-$i",
            mediaTitle = "Example Movie $i",
            mediaType = MediaType.MOVIE,
            state = DownloadItemState.entries[i % DownloadItemState.entries.size],
            streamTotalBytes = 1_000_000L,
            streamBytesDownloaded = 500_000L,
        )
    }

    FlixclusiveTheme {
        Surface {
            DownloadsTweakScreenContent(
                entries = Async.Success(items.map { DownloadListEntry.Single(it) }),
                query = "",
                stateFilters = emptySet(),
                typeFilters = emptySet(),
                onNavigateBack = {},
                onQueryChange = {},
                onToggleStateFilter = {},
                onToggleTypeFilter = {},
                onPause = {},
                onResume = {},
                onStop = {},
                onRetry = {},
                onDelete = {},
                onOpen = {},
                onPauseBatch = { _, _ -> },
                onStopBatch = { _, _ -> },
            )
        }
    }
}
