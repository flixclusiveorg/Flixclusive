package com.flixclusive.feature.mobile.repository.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.navigation.navigator.RepositorySearchScreenNavigator
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.util.getFeedbackOnLongPress
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.core.ui.mobile.util.showMessage
import com.flixclusive.feature.mobile.repository.search.component.AddRepositoryBar
import com.flixclusive.feature.mobile.repository.search.component.RemoveAlertDialog
import com.flixclusive.feature.mobile.repository.search.component.RepositoryCard
import com.flixclusive.feature.mobile.repository.search.component.RepositorySearchTopBar
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.locale.R as LocaleR

@Destination
@Composable
internal fun RepositorySearchScreen(
    navigator: RepositorySearchScreenNavigator
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val viewModel = hiltViewModel<RepositorySearchScreenViewModel>()
    val repositories by viewModel.repositories.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val shouldShowTopBar by listState.isScrollingUp()

    val isSelecting = rememberSaveable { mutableStateOf(false) }
    val isRemoving = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isSelecting.value) {
        if (!isSelecting.value)
            viewModel.clearSelection()
        else focusManager.clearFocus()
    }

    LaunchedEffect(viewModel.selectedRepositories.size) {
        if (viewModel.selectedRepositories.size == 0)
            isSelecting.value = false
    }

    val hasQueryBoxError = remember(viewModel.errorMessage.value) {
        mutableStateOf(viewModel.errorMessage.value != null)
    }

    val hapticFeedBack = getFeedbackOnLongPress()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.errorMessage.value) {
        if (viewModel.errorMessage.value != null) {
            val message = viewModel.errorMessage.value!!.error?.asString(context)
                ?: context.getString(LocaleR.string.default_error)

            snackbarHostState.showMessage(message)
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            RepositorySearchTopBar(
                isVisible = shouldShowTopBar,
                isSelecting = isSelecting,
                selectCount = viewModel.selectedRepositories.size,
                onRemoveRepositories = { isRemoving.value = true },
                onNavigationIconClick = navigator::goBack
            )
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
            ) {
                item {
                    AddRepositoryBar(
                        urlQuery = viewModel.urlQuery,
                        isError = hasQueryBoxError,
                        focusRequester = focusRequester,
                        onAdd = viewModel::onAddLink
                    )
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(vertical = 10.dp),
                        thickness = 1.dp,
                        color = LocalContentColor.current.onMediumEmphasis(0.4F)
                    )
                }

                items(repositories) { repository ->
                    val isSelected = remember(viewModel.selectedRepositories.size) {
                        viewModel.selectedRepositories.contains(repository)
                    }

                    RepositoryCard(
                        repository = repository,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelecting.value && !isSelected) {
                                viewModel.selectRepository(repository)
                                return@RepositoryCard
                            } else if (isSelecting.value) {
                                viewModel.unselectRepository(repository)
                                return@RepositoryCard
                            }

                            navigator.openRepositoryScreen(repository)
                        },
                        onLongClick = {
                            if (isSelecting.value) {
                                return@RepositoryCard
                            }

                            hapticFeedBack()
                            viewModel.selectRepository(repository)
                            isSelecting.value = true
                        },
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                    )
                }
            }
        }
    }

    if (isRemoving.value) {
        RemoveAlertDialog(
            confirm = {
                viewModel.onRemoveRepositories()
                isRemoving.value = false
            },
            cancel = {
                isSelecting.value = false
                isRemoving.value = false
            }
        )
    }
}
