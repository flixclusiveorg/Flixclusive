package com.flixclusive.feature.mobile.repository.manage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.presentation.common.util.CustomClipboardManager.Companion.rememberClipboardManager
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.material3.dialog.IconAlertDialog
import com.flixclusive.core.presentation.mobile.extensions.fillMaxAdaptiveWidth
import com.flixclusive.core.presentation.mobile.extensions.showMessage
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveGridCellsCount
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.presentation.mobile.util.getFeedbackOnLongPress
import com.flixclusive.feature.mobile.repository.manage.component.AddRepositoryBar
import com.flixclusive.feature.mobile.repository.manage.component.RepositoryCard
import com.flixclusive.feature.mobile.repository.manage.component.RepositoryManagerTopBar
import com.flixclusive.model.provider.Repository
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Destination
@Composable
internal fun RepositoryManagerScreen(
    navigator: GoBackAction,
    viewModel: RepositoryManagerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val repositories by viewModel.repositories.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedRepositories by viewModel.selectedRepositories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val urlQuery by viewModel.urlQuery.collectAsStateWithLifecycle()

    RepositoryManagerScreenContent(
        uiState = uiState,
        repositories = {
            if (searchQuery.isNotBlank() && uiState.isShowingSearchBar) {
                searchResults
            } else {
                repositories
            }
        },
        searchQuery = { searchQuery },
        urlQuery = { urlQuery },
        selectedRepositories = { selectedRepositories },
        onAddLink = viewModel::onAddLink,
        clearSelection = viewModel::clearSelection,
        toggleRepositorySelection = viewModel::toggleRepositorySelection,
        onGoBack = navigator::goBack,
        onRemoveRepository = viewModel::onRemoveRepository,
        onRemoveSelection = viewModel::onRemoveSelection,
        onConsumeError = viewModel::onConsumeError,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onUrlQueryChange = viewModel::onUrlQueryChange,
        onToggleSearchBar = viewModel::onToggleSearchBar,
    )
}

@Composable
private fun RepositoryManagerScreenContent(
    uiState: RepositoryManagerUiState,
    repositories: () -> List<Repository>,
    searchQuery: () -> String,
    urlQuery: () -> String,
    selectedRepositories: () -> ImmutableSet<Repository>,
    onAddLink: () -> Unit,
    onRemoveRepository: (Repository) -> Unit,
    onRemoveSelection: () -> Unit,
    onConsumeError: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onUrlQueryChange: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    clearSelection: () -> Unit,
    toggleRepositorySelection: (Repository) -> Unit,
    onGoBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = rememberClipboardManager()

    val hapticFeedBack = getFeedbackOnLongPress()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var isFocusedInitialized by rememberSaveable { mutableStateOf(false) }
    var isAlertDialogOpened by rememberSaveable { mutableStateOf(false) }
    var repositoryToRemove by remember { mutableStateOf<Repository?>(null) }

    val isListOfRepositoryEmpty by remember {
        derivedStateOf { repositories().isEmpty() }
    }

    val selectedRepositoriesSize by remember {
        derivedStateOf { selectedRepositories().size }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            val message = uiState.error
                .asString(context)
                .takeIf { it.isNotBlank() }
                ?: context.getString(LocaleR.string.default_error)

            snackbarHostState.showMessage(message)
        }
    }

    LaunchedEffect(Unit) {
        if (isFocusedInitialized) {
            focusRequester.requestFocus()
            isFocusedInitialized = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            RepositoryManagerTopBar(
                isSelecting = selectedRepositoriesSize > 0,
                selectCount = selectedRepositoriesSize,
                searchQuery = searchQuery,
                isSearching = uiState.isShowingSearchBar,
                onRemoveRepositories = { isAlertDialogOpened = true },
                onCopyRepositories = {
                    val repositoryUrls = selectedRepositories().joinToString("\n") { it.url }
                    clipboardManager.setText(repositoryUrls)
                },
                onNavigationClick = onGoBack,
                onCollapseTopBar = clearSelection,
                onQueryChange = onSearchQueryChange,
                onToggleSearchBar = onToggleSearchBar,
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(LocalGlobalScaffoldPadding.current),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                contentPadding = it,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                columns = getAdaptiveGridCellsCount(),
                modifier = Modifier.padding(horizontal = 10.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AddRepositoryBar(
                        urlQuery = urlQuery,
                        isParseError = uiState.error != null,
                        focusRequester = focusRequester,
                        onAdd = onAddLink,
                        onUrlQueryChange = onUrlQueryChange,
                        onConsumeError = onConsumeError,
                        modifier = Modifier
                            .fillMaxAdaptiveWidth()
                            .align(Alignment.Center),
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                items(repositories(), key = { it.url }) { repository ->
                    val isSelected by remember(selectedRepositories()) {
                        derivedStateOf {
                            selectedRepositories().contains(repository)
                        }
                    }

                    RepositoryCard(
                        repository = repository,
                        isSelected = isSelected,
                        onClick = {
                            val isSelecting = selectedRepositories().isNotEmpty()
                            if (isSelecting) {
                                toggleRepositorySelection(repository)
                                return@RepositoryCard
                            }
                        },
                        onLongClick = {
                            val isSelecting = selectedRepositories().isNotEmpty()
                            if (isSelecting) return@RepositoryCard

                            hapticFeedBack()
                            toggleRepositorySelection(repository)
                        },
                        onDeleteRepository = {
                            isAlertDialogOpened = true
                            repositoryToRemove = repository
                        },
                        onCopy = clipboardManager::setText,
                        modifier =
                            Modifier
                                .padding(vertical = 5.dp)
                                .animateItem(),
                    )
                }
            }

            AnimatedVisibility(
                visible = isListOfRepositoryEmpty,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .padding(it),
            ) {
                EmptyDataMessage(
                    description = stringResource(LocaleR.string.empty_repositories_list_message),
                    modifier = Modifier.alpha(0.8F),
                )
            }
        }
    }

    if (isAlertDialogOpened) {
        IconAlertDialog(
            painter = painterResource(UiCommonR.drawable.warning_outline),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            description = stringResource(LocaleR.string.action_warning_default_message),
            onConfirm = {
                if (repositoryToRemove != null) {
                    onRemoveRepository(repositoryToRemove!!)
                } else {
                    onRemoveSelection()
                }
            },
            onDismiss = {
                isAlertDialogOpened = false
                repositoryToRemove = null
                onRemoveSelection()
            },
        )
    }
}

@Preview
@Composable
private fun RepositoryManagerScreenBasePreview() {
    FlixclusiveTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val sampleRepositories = remember {
                listOf(
                    Repository(
                        name = "FlixHQ",
                        owner = "flixclusive-dev",
                        url = "https://raw.githubusercontent.com/flixclusive-dev/flixhq-provider/" +
                            "main/repository.json",
                        rawLinkFormat = "https://raw.githubusercontent.com/flixclusive-dev/flixhq-provider/" +
                            "%branch%/%filename%",
                    ),
                    Repository(
                        name = "SuperStream",
                        owner = "superstream-dev",
                        url = "https://raw.githubusercontent.com/superstream-dev/superstream-provider/" +
                            "main/repository.json",
                        rawLinkFormat = "https://raw.githubusercontent.com/superstream-dev/superstream-provider/" +
                            "%branch%/%filename%",
                    ),
                    Repository(
                        name = "MovieBox",
                        owner = "moviebox-dev",
                        url = "https://raw.githubusercontent.com/moviebox-dev/moviebox-provider/" +
                            "main/repository.json",
                        rawLinkFormat = "https://raw.githubusercontent.com/moviebox-dev/moviebox-provider/" +
                            "%branch%/%filename%",
                    ),
                )
            }

            var searchQuery by remember { mutableStateOf("") }
            var urlQuery by remember { mutableStateOf("") }
            var selectedRepositories by remember {
                mutableStateOf(persistentSetOf<Repository>())
            }
            var uiState by remember {
                mutableStateOf(RepositoryManagerUiState())
            }

            RepositoryManagerScreenContent(
                uiState = uiState,
                repositories = { sampleRepositories },
                searchQuery = { searchQuery },
                urlQuery = { urlQuery },
                selectedRepositories = { selectedRepositories },
                onAddLink = {
                    if (urlQuery.isNotBlank()) {
                        urlQuery = ""
                    }
                },
                clearSelection = {
                    selectedRepositories = persistentSetOf()
                },
                toggleRepositorySelection = { repository ->
                    selectedRepositories = if (selectedRepositories.contains(repository)) {
                        selectedRepositories.remove(repository)
                    } else {
                        selectedRepositories.add(repository)
                    }
                },
                onGoBack = { /* Preview - no action */ },
                onRemoveRepository = { repository ->
                    selectedRepositories = selectedRepositories.remove(repository)
                },
                onRemoveSelection = {
                    selectedRepositories = persistentSetOf()
                },
                onConsumeError = {
                    uiState = uiState.copy(error = null)
                },
                onSearchQueryChange = { query ->
                    searchQuery = query
                },
                onUrlQueryChange = { query ->
                    urlQuery = query
                },
                onToggleSearchBar = { isShowing ->
                    uiState = uiState.copy(isShowingSearchBar = isShowing)
                },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun RepositoryManagerScreenCompactLandscapePreview() {
    RepositoryManagerScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun RepositoryManagerScreenMediumPortraitPreview() {
    RepositoryManagerScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun RepositoryManagerScreenMediumLandscapePreview() {
    RepositoryManagerScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun RepositoryManagerScreenExtendedPortraitPreview() {
    RepositoryManagerScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun RepositoryManagerScreenExtendedLandscapePreview() {
    RepositoryManagerScreenBasePreview()
}
