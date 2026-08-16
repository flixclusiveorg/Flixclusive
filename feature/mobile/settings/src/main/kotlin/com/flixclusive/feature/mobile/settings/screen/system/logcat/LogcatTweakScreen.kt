package com.flixclusive.feature.mobile.settings.screen.system.logcat

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.presentation.common.util.CustomClipboardManager.Companion.rememberClipboardManager
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.LoadingScreen
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.components.material3.dialog.TextAlertDialog
import com.flixclusive.core.presentation.mobile.components.material3.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarWithSearch
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.feature.mobile.settings.R
import com.flixclusive.feature.mobile.settings.screen.system.logcat.component.LogcatActionsSheet
import com.flixclusive.feature.mobile.settings.screen.system.logcat.component.LogcatFilterField
import com.flixclusive.feature.mobile.settings.screen.system.logcat.component.LogcatFindBar
import com.flixclusive.feature.mobile.settings.screen.system.logcat.component.LogcatRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.flixclusive.core.drawables.R as UiCommonR

private val LogRowHeight = 18.dp
private const val INSTANT_JUMP_THRESHOLD = 50
private val EmptyLogcatUiState = LogcatUiState()

@Destination<ExternalModuleGraph>
@Composable
internal fun LogcatTweakScreen(
    navigator: NavigateBack,
    viewModel: LogcatTweakViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val levelFilters by viewModel.levelFilters.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()

    LogcatTweakScreenContent(
        state = state,
        // Read once as the fields' seed values rather than collected as State — each field owns its
        // own text after that, so neither query needs to be observed here.
        initialFilterQuery = viewModel.filterQuery.value,
        searchQuery = { viewModel.searchQuery.value },
        levelFilters = levelFilters,
        isPaused = isPaused,
        onNavigateBack = navigator::navigateBack,
        onFilterQueryChange = viewModel::onFilterQueryChange,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onToggleLevelFilter = viewModel::onToggleLevelFilter,
        onTogglePause = viewModel::onTogglePause,
        onClear = viewModel::onClear,
        onReload = viewModel::onReload,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogcatTweakScreenContent(
    state: Async<LogcatUiState>,
    initialFilterQuery: String,
    searchQuery: () -> String,
    levelFilters: ImmutableSet<LogLevel>,
    isPaused: Boolean,
    onNavigateBack: () -> Unit,
    onFilterQueryChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleLevelFilter: (LogLevel) -> Unit,
    onTogglePause: () -> Unit,
    onClear: () -> Unit,
    onReload: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = rememberClipboardManager()
    val scope = rememberCoroutineScope()

    val uiState = (state as? Async.Success)?.data ?: EmptyLogcatUiState

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var isShowingActions by rememberSaveable { mutableStateOf(false) }
    var isConfirmingClear by rememberSaveable { mutableStateOf(false) }
    var currentMatch by rememberSaveable { mutableIntStateOf(0) }
    var followTail by rememberSaveable { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val rowHeightPx = with(LocalDensity.current) { LogRowHeight.roundToPx() }

    suspend fun jumpTo(ordinal: Int) {
        val target = uiState.matchIndices.getOrNull(ordinal) ?: return
        followTail = false

        val centering = -(listState.layoutInfo.viewportSize.height / 2 - rowHeightPx / 2)
        // Animating across thousands of rows is a multi-second crawl, so only near jumps animate.
        if (abs(target - listState.firstVisibleItemIndex) > INSTANT_JUMP_THRESHOLD) {
            listState.scrollToItem(target, centering)
        } else {
            listState.animateScrollToItem(target, centering)
        }
    }

    // Keyed on the query alone, so an incoming batch cannot yank the list out from under the user.
    // The state carries its query and its match indices from the same emission, so they always agree.
    LaunchedEffect(uiState.searchQuery) {
        currentMatch = 0
        if (uiState.searchQuery.isNotEmpty()) jumpTo(0)
    }

    if (isConfirmingClear) {
        TextAlertDialog(
            title = stringResource(R.string.logcat_clear_confirm_title),
            message = stringResource(R.string.logcat_clear_confirm_message),
            confirmButtonLabel = stringResource(R.string.logcat_clear),
            onConfirm = {
                onClear()
                isConfirmingClear = false
            },
            onDismiss = { isConfirmingClear = false },
        )
    }

    if (isShowingActions) {
        LogcatActionsSheet(
            onDismissRequest = { isShowingActions = false },
            onCopy = {
                clipboardManager.setText(uiState.entries.joinToString("\n") { it.text })
                isShowingActions = false
            },
            onShare = {
                context.shareLogs(uiState.entries.joinToString("\n") { it.text })
                isShowingActions = false
            },
            onClear = {
                isShowingActions = false
                isConfirmingClear = true
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(),
        topBar = {
            CommonTopBarWithSearch(
                title = stringResource(R.string.logcat),
                isSearching = isSearching,
                searchQuery = searchQuery,
                onQueryChange = onSearchQueryChange,
                onToggleSearchBar = {
                    isSearching = it
                    if (!it) onSearchQueryChange("")
                },
                onNavigate = onNavigateBack,
                scrollBehavior = scrollBehavior,
                extraActions = {
                    PlainTooltipBox(
                        description = stringResource(
                            if (isPaused) R.string.logcat_resume else R.string.logcat_pause,
                        ),
                    ) {
                        ActionButton(onClick = onTogglePause) {
                            AdaptiveIcon(
                                painter = painterResource(
                                    if (isPaused) UiCommonR.drawable.play else UiCommonR.drawable.round_pause_24,
                                ),
                                contentDescription = stringResource(
                                    if (isPaused) R.string.logcat_resume else R.string.logcat_pause,
                                ),
                                dp = 18.dp,
                            )
                        }
                    }

                    PlainTooltipBox(description = stringResource(R.string.logcat_more_actions)) {
                        ActionButton(onClick = { isShowingActions = true }) {
                            AdaptiveIcon(
                                painter = painterResource(UiCommonR.drawable.more_vert),
                                contentDescription = stringResource(R.string.logcat_more_actions),
                                dp = 18.dp,
                            )
                        }
                    }
                },
            )
        },
        modifier = Modifier
            .padding(LocalGlobalScaffoldPadding.current)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // The top bar hides its extra actions while searching, so the find controls need a bar
            // of their own rather than a slot up there.
            AnimatedVisibility(visible = isSearching) {
                LogcatFindBar(
                    matchCount = uiState.matchIndices.size,
                    currentMatch = currentMatch,
                    onPrevious = {
                        scope.launch {
                            currentMatch = (currentMatch - 1).mod(uiState.matchIndices.size)
                            jumpTo(currentMatch)
                        }
                    },
                    onNext = {
                        scope.launch {
                            currentMatch = (currentMatch + 1).mod(uiState.matchIndices.size)
                            jumpTo(currentMatch)
                        }
                    },
                )
            }

            LogcatFilterField(
                initialQuery = initialFilterQuery,
                onQueryChange = onFilterQueryChange,
                levelFilters = levelFilters,
                onToggleLevelFilter = onToggleLevelFilter,
                filterError = uiState.filterError,
                modifier = Modifier.padding(top = 4.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            LogcatList(
                state = state,
                uiState = uiState,
                listState = listState,
                horizontalScroll = horizontalScroll,
                currentMatchIndex = uiState.matchIndices.getOrNull(currentMatch),
                followTail = followTail,
                onFollowTailChange = { followTail = it },
                onReload = onReload,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun LogcatList(
    state: Async<LogcatUiState>,
    uiState: LogcatUiState,
    listState: LazyListState,
    horizontalScroll: ScrollState,
    currentMatchIndex: Int?,
    followTail: Boolean,
    onFollowTailChange: (Boolean) -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val measurer = rememberTextMeasurer()
    val logStyle = remember {
        TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
    }
    val charWidthPx = remember(logStyle) { measurer.measure("0", logStyle).size.width }

    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val matchStyle = remember(highlightColor) { SpanStyle(background = highlightColor) }

    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount == 0 ||
                info.visibleItemsInfo.lastOrNull()?.index == info.totalItemsCount - 1
        }
    }

    val setFollowTail by rememberUpdatedState(onFollowTailChange)

    LaunchedEffect(listState) {
        snapshotFlow { listState.lastScrolledBackward }
            .filter { it }
            .collect { setFollowTail(false) }
    }

    LaunchedEffect(listState) {
        snapshotFlow { isAtBottom }
            .filter { it }
            .collect { setFollowTail(true) }
    }

    // scrollToItem rather than animateScrollToItem: with a batch arriving every ~150ms an animation
    // would be cancelled before finishing and the list would crawl instead of keeping up.
    LaunchedEffect(uiState.entries.lastOrNull()?.id, followTail) {
        if (followTail && uiState.entries.isNotEmpty()) {
            listState.scrollToItem(uiState.entries.lastIndex)
        }
    }

    Box(modifier = modifier) {
        AsyncAnimatedContent(
            targetState = state,
            modifier = Modifier.fillMaxSize(),
            loadingContent = { LoadingScreen(modifier = Modifier.fillMaxSize()) },
            errorContent = {
                RetryButton(
                    modifier = Modifier.fillMaxSize(),
                    error = it.message.asString(),
                    onRetry = onReload,
                )
            },
        ) { stateProvider ->
            val logs = stateProvider()

            if (logs.entries.isEmpty()) {
                EmptyDataMessage(
                    modifier = Modifier.fillMaxSize(),
                    emojiHeader = "🪵",
                    title = stringResource(R.string.logcat_empty_title),
                    description = stringResource(R.string.logcat_empty_description),
                )
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    // Every row is measured at this one width. Sharing a ScrollState across N rows
                    // means N nodes write its extent during measure, so they have to agree or the
                    // shared offset flips between the longest and shortest visible line each frame.
                    val rowWidth = with(LocalDensity.current) {
                        maxOf((charWidthPx * logs.maxLineLength).toDp(), maxWidth)
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = logs.entries,
                            key = { it.id },
                            contentType = { "log" },
                        ) { entry ->
                            LogcatRow(
                                entry = entry,
                                searchQuery = logs.searchQuery,
                                isCurrentMatch = currentMatchIndex != null &&
                                    logs.entries.getOrNull(currentMatchIndex)?.id == entry.id,
                                style = logStyle,
                                width = rowWidth,
                                height = LogRowHeight,
                                horizontalScroll = horizontalScroll,
                                matchStyle = matchStyle,
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !followTail,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            PlainTooltipBox(description = stringResource(R.string.logcat_scroll_to_latest)) {
                ActionButton(
                    onClick = {
                        onFollowTailChange(true)
                        scope.launch {
                            if (uiState.entries.isNotEmpty()) {
                                listState.scrollToItem(uiState.entries.lastIndex)
                            }
                        }
                    },
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    AdaptiveIcon(
                        painter = painterResource(UiCommonR.drawable.down_arrow),
                        contentDescription = stringResource(R.string.logcat_scroll_to_latest),
                        dp = 18.dp,
                    )
                }
            }
        }
    }
}

private fun android.content.Context.shareLogs(logs: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.logcat_share_title))
        putExtra(Intent.EXTRA_TEXT, logs)
    }

    startActivity(Intent.createChooser(intent, getString(R.string.logcat_share)))
}

@Preview
@Composable
private fun LogcatTweakScreenPreview() {
    val entries = List(30) { index ->
        val level = LogLevel.entries[index % LogLevel.entries.size]
        LogcatParser.parse(
            raw = "06-14 09:12:3$index.123  1234  1256 ${level.letter} Flixclusive: sample line $index",
            id = index.toLong(),
            previous = null,
        )!!
    }

    FlixclusiveTheme {
        Surface {
            LogcatTweakScreenContent(
                state = Async.Success(
                    LogcatUiState(
                        entries = entries.toPersistentList(),
                        maxLineLength = entries.maxOf { it.text.length },
                        totalCount = entries.size,
                    ),
                ),
                initialFilterQuery = LogcatTweakViewModel.DEFAULT_FILTER_QUERY,
                searchQuery = { "" },
                levelFilters = persistentSetOf(),
                isPaused = false,
                onNavigateBack = {},
                onFilterQueryChange = {},
                onSearchQueryChange = {},
                onToggleLevelFilter = {},
                onTogglePause = {},
                onClear = {},
                onReload = {},
            )
        }
    }
}
