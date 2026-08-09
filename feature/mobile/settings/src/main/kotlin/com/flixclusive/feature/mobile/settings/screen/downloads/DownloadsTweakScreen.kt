package com.flixclusive.feature.mobile.settings.screen.downloads

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.navigation.navargs.PlaybackRequest
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToMediaLinksBottomSheet
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.feature.mobile.settings.component.AnimatedFilterChip
import com.flixclusive.core.presentation.mobile.components.LoadingScreen
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.material3.dialog.TextAlertDialog
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarWithSearch
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.model.media.common.MediaType
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.coroutines.flow.collectLatest
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
                    PlaybackRequest.FromDownload(downloadItemId = event.itemId),
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
        onReloadList = viewModel::onReloadList,
    )
}

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
    onReloadList: () -> Unit,
) {
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
                onPause = onPause,
                onResume = onResume,
                onStop = onStop,
                onRetry = onRetry,
                onDelete = onDelete,
                onOpen = onOpen,
                onPauseBatch = onPauseBatch,
                onStopBatch = onStopBatch,
                onReloadList = onReloadList,
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

/** Isolated so entries/expansion updates don't force the top bar or filter row to recompose. */
@Composable
private fun DownloadsEntriesList(
    entries: Async<List<DownloadListEntry>>,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onStop: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onPauseBatch: (String, Int) -> Unit,
    onStopBatch: (String, Int) -> Unit,
    onReloadList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Deleting takes the downloaded files with it and there is no undo, so the tap only arms the
    // dialog; nothing is removed until it's confirmed.
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    pendingDeleteId?.let { itemId ->
        TextAlertDialog(
            title = stringResource(LocaleR.string.download_delete_confirm_title),
            message = stringResource(LocaleR.string.download_delete_confirm_message),
            confirmButtonLabel = stringResource(LocaleR.string.delete),
            onConfirm = {
                onDelete(itemId)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null },
        )
    }

    AsyncAnimatedContent(
        targetState = entries,
        modifier = modifier,
        loadingContent = { LoadingScreen(modifier = Modifier.fillMaxSize()) },
        errorContent = {
            RetryButton(
                modifier = Modifier.fillMaxSize(),
                error = it.message.asString(),
                onRetry = onReloadList,
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
                items(
                    items = entriesProvider(),
                    key = { it.key() }
                ) { entry ->
                    when (entry) {
                        is DownloadListEntry.Single -> DownloadItemCard(
                            item = entry.item,
                            onPause = { onPause(entry.item.id) },
                            onResume = { onResume(entry.item.id) },
                            onStop = { onStop(entry.item.id) },
                            onRetry = { onRetry(entry.item.id) },
                            onDelete = { pendingDeleteId = entry.item.id },
                            onOpen = { onOpen(entry.item) },
                            modifier = Modifier.animateItem()
                        )

                        is DownloadListEntry.Batch -> {
                            var isExpanded by rememberSaveable { mutableStateOf(false) }

                            DownloadBatchGroup(
                                entry = entry,
                                isExpanded = isExpanded,
                                onToggleExpand = { isExpanded = !isExpanded },
                                onPauseBatch = { onPauseBatch(entry.mediaId, entry.seasonNumber) },
                                onStopBatch = { onStopBatch(entry.mediaId, entry.seasonNumber) },
                                onPause = onPause,
                                onResume = onResume,
                                onStop = onStop,
                                onRetry = onRetry,
                                onDelete = { pendingDeleteId = it },
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
                onReloadList = {},
            )
        }
    }
}
