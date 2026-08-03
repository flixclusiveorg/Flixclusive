package com.flixclusive.feature.mobile.settings.screen.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.flixclusive.core.navigation.navigator.NavigateToMediaLinksBottomSheet
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarWithSearch
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.feature.mobile.settings.util.CacheLinksFormatUtil
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.coroutines.flow.collectLatest
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

interface NavigatorDownloadsTweakScreen :
    NavigateBack,
    NavigateToMediaLinksBottomSheet

@Destination<ExternalModuleGraph>
@Composable
internal fun DownloadsTweakScreen(
    navigator: NavigatorDownloadsTweakScreen,
    viewModel: DownloadsTweakViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val stateFilters by viewModel.stateFilters.collectAsStateWithLifecycle()
    val typeFilters by viewModel.typeFilters.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is DownloadsTweakEvent.OpenFile -> navigator.showPlayerSplashScreen(
                    media = event.item.toPlayableMediaMetadata(),
                    episode = event.item.toPlayableEpisode(),
                    initialStreamUrl = event.file.uri.toString(),
                )
            }
        }
    }

    DownloadsTweakScreenContent(
        entries = entries,
        // Read once as the search field's seed value rather than collected as State — the field
        // owns its own text state after that, so the query doesn't need to be observed here.
        searchQuery = { viewModel.query.value },
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

/**
 * Downloads don't retain the provider they were fetched from, so a minimal, non-partial
 * [MediaMetadata] is synthesized here purely to satisfy the player's nav args — the player
 * falls back to [initialStreamUrl] as the local file to play instead of resolving links.
 */
private fun DownloadItem.toPlayableMediaMetadata(): MediaMetadata = when (mediaType) {
    MediaType.MOVIE -> Movie(
        id = mediaId,
        title = mediaTitle,
        providerId = LOCAL_DOWNLOAD_PROVIDER_ID,
        posterImage = null,
    )

    MediaType.SHOW -> Show(
        id = mediaId,
        title = mediaTitle,
        providerId = LOCAL_DOWNLOAD_PROVIDER_ID,
        posterImage = null,
        seasons = emptyList(),
        totalEpisodes = 0,
        totalSeasons = 0,
    )
}

private fun DownloadItem.toPlayableEpisode(): Episode? {
    val season = seasonNumber ?: return null
    val episode = episodeNumber ?: return null

    return Episode(
        id = "$mediaId-$season-$episode",
        number = episode,
        season = season,
        isReleased = true,
        title = CacheLinksFormatUtil.getFormattedTitle(season, episode),
    )
}

private const val LOCAL_DOWNLOAD_PROVIDER_ID = "local-download"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsTweakScreenContent(
    entries: Async<List<DownloadListEntry>>,
    searchQuery: () -> String,
    stateFilters: Set<DownloadStateFilter>,
    typeFilters: Set<MediaType>,
    onNavigateBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleStateFilter: (DownloadStateFilter) -> Unit,
    onToggleTypeFilter: (MediaType) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onStop: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onPauseBatch: (String, Int) -> Unit,
    onStopBatch: (String, Int) -> Unit,
) {
    var expandedBatches by remember { mutableStateOf(setOf<String>()) }
    var isSearching by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        contentWindowInsets = WindowInsets(),
        topBar = {
            CommonTopBarWithSearch(
                title = stringResource(LocaleR.string.downloads),
                isSearching = isSearching,
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                onToggleSearchBar = { isSearching = it },
                onNavigate = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier
            .padding(LocalGlobalScaffoldPadding.current)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            DownloadsFilterRow(
                stateFilters = stateFilters,
                typeFilters = typeFilters,
                onToggleStateFilter = onToggleStateFilter,
                onToggleTypeFilter = onToggleTypeFilter,
            )

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            DownloadsEntriesList(
                entries = entries,
                expandedBatches = expandedBatches,
                onToggleExpand = { key -> expandedBatches = expandedBatches.toggleBatch(key) },
                onPause = onPause,
                onResume = onResume,
                onStop = onStop,
                onRetry = onRetry,
                onDelete = onDelete,
                onOpen = onOpen,
                onPauseBatch = onPauseBatch,
                onStopBatch = onStopBatch,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Isolated so a filter toggle only recomposes this row, not the top bar or the entries list. */
@Composable
private fun DownloadsFilterRow(
    stateFilters: Set<DownloadStateFilter>,
    typeFilters: Set<MediaType>,
    onToggleStateFilter: (DownloadStateFilter) -> Unit,
    onToggleTypeFilter: (MediaType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(DownloadStateFilter.entries, key = { "state-${it.name}" }) { filter ->
            AnimatedFilterChip(
                selected = filter in stateFilters,
                label = filter.label(),
                onClick = { onToggleStateFilter(filter) },
            )
        }

        items(MediaType.entries, key = { "type-${it.name}" }) { type ->
            AnimatedFilterChip(
                selected = type in typeFilters,
                label = type.label(),
                onClick = { onToggleTypeFilter(type) },
            )
        }
    }
}

@Composable
private fun AnimatedFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = CircleShape,
        leadingIcon = {
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(150)) + expandHorizontally(tween(150)),
                exit = fadeOut(tween(150)) + shrinkHorizontally(tween(150)),
            ) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = MaterialTheme.colorScheme.surface
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected),
        modifier = modifier.animateContentSize(),
    )
}

/** Isolated so entries/expansion updates don't force the top bar or filter row to recompose. */
@Composable
private fun DownloadsEntriesList(
    entries: Async<List<DownloadListEntry>>,
    expandedBatches: Set<String>,
    onToggleExpand: (String) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onStop: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onPauseBatch: (String, Int) -> Unit,
    onStopBatch: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncAnimatedContent(
        targetState = entries,
        modifier = modifier,
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
                            onToggleExpand = { onToggleExpand(entry.batchKey()) },
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
            ownerId = "owner",
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
                searchQuery = { "" },
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
