package com.flixclusive.feature.mobile.repository.manage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.dialog.IconAlertDialog
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewRepositoryAction
import com.flixclusive.core.ui.common.util.CustomClipboardManager.Companion.rememberClipboardManager
import com.flixclusive.core.ui.mobile.component.EmptyDataMessage
import com.flixclusive.core.ui.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.ui.mobile.util.getFeedbackOnLongPress
import com.flixclusive.core.ui.mobile.util.showMessage
import com.flixclusive.feature.mobile.repository.manage.component.AddRepositoryBar
import com.flixclusive.feature.mobile.repository.manage.component.RepositoryCard
import com.flixclusive.feature.mobile.repository.manage.component.RepositoryManagerTopBar
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

interface RepositoryManagerScreenNavigator :
    ViewRepositoryAction,
    GoBackAction

@Destination
@Composable
internal fun RepositoryManagerScreen(
    navigator: RepositoryManagerScreenNavigator,
    viewModel: RepositoryManagerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedRepositories by viewModel.selectedRepositories.collectAsStateWithLifecycle()

    val clipboardManager = rememberClipboardManager()

    val hapticFeedBack = getFeedbackOnLongPress()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            val message =
                uiState.error!!
                    .error
                    ?.asString(context)
                    ?: context.getString(LocaleR.string.default_error)

            snackbarHostState.showMessage(message)
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (uiState.isFocusedInitialized) {
            focusRequester.requestFocus()
            viewModel.onInitializeFocus()
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier =
            Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(LocalGlobalScaffoldPadding.current),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            RepositoryManagerTopBar(
                isSelecting = selectedRepositories.isNotEmpty(),
                selectCount = selectedRepositories.size,
                searchQuery = { uiState.searchQuery },
                isSearching = uiState.isShowingSearchBar,
                onRemoveRepositories = { viewModel.onToggleAlertDialog(true) },
                onCopyRepositories = {
                    val repositoryUrls = selectedRepositories.joinToString("\n") { it.url }
                    clipboardManager.setText(repositoryUrls)
                },
                onNavigationClick = navigator::goBack,
                onCollapseTopBar = viewModel::clearSelection,
                onQueryChange = viewModel::onSearchQueryChange,
                onToggleSearchBar = viewModel::onToggleSearchBar,
                scrollBehavior = scrollBehavior,
            )
        },
    ) {
        Box(
            modifier =
                Modifier
                    .padding(it)
                    .fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .padding(horizontal = 10.dp),
            ) {
                item {
                    AddRepositoryBar(
                        urlQuery = uiState.urlQuery,
                        isParseError = uiState.error != null,
                        focusRequester = focusRequester,
                        onAdd = viewModel::onAddLink,
                        onUrlQueryChange = viewModel::onUrlQueryChange,
                        onConsumeError = viewModel::onConsumeError,
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                items(uiState.repositories, key = { it.url }) { repository ->
                    RepositoryCard(
                        repository = repository,
                        isSelected = selectedRepositories.contains(repository),
                        onClick = {
                            val isSelected = selectedRepositories.contains(repository)
                            val isSelecting = selectedRepositories.isNotEmpty()

                            if (isSelecting && !isSelected) {
                                viewModel.selectRepository(repository)
                                return@RepositoryCard
                            } else if (isSelecting) {
                                viewModel.unselectRepository(repository)
                                return@RepositoryCard
                            }

                            navigator.openRepositoryDetails(repository)
                        },
                        onLongClick = {
                            val isSelecting = selectedRepositories.isNotEmpty()
                            if (isSelecting) return@RepositoryCard

                            hapticFeedBack()
                            viewModel.selectRepository(repository)
                        },
                        onDeleteRepository = { viewModel.onToggleAlertDialog(true, repository) },
                        onCopy = clipboardManager::setText,
                        modifier =
                            Modifier
                                .padding(vertical = 5.dp)
                                .animateItem(),
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.repositories.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(),
            ) {
                EmptyDataMessage(
                    description = stringResource(LocaleR.string.empty_repositories_list_message),
                    modifier = Modifier.alpha(0.8F),
                )
            }
        }
    }

    if (uiState.isShowingAlertDialog) {
        IconAlertDialog(
            painter = painterResource(UiCommonR.drawable.warning_outline),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            description = stringResource(LocaleR.string.action_warning_default_message),
            onConfirm = {
                if (uiState.singleRepositoryToRemove != null) {
                    viewModel.onRemoveRepository(uiState.singleRepositoryToRemove!!)
                } else {
                    viewModel.onRemoveRepositories()
                }
            },
            onDismiss = {
                viewModel.onToggleAlertDialog(false)
            },
        )
    }
}
