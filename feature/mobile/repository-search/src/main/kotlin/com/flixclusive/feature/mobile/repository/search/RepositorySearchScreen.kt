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
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.util.getFeedbackOnLongPress
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.core.ui.mobile.util.showMessage
import com.flixclusive.feature.mobile.repository.search.component.AddRepositoryBar
import com.flixclusive.feature.mobile.repository.search.component.RepositoryCard
import com.flixclusive.feature.mobile.repository.search.component.RepositorySearchTopBar
import com.flixclusive.gradle.entities.Repository
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import com.flixclusive.core.util.R as UtilR

interface RepositorySearchScreenNavigator : GoBackAction {
    fun openRepositoryScreen(repository: Repository)
}

@Destination
@Composable
fun RepositorySearchScreen(
    navigator: RepositorySearchScreenNavigator
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<RepositorySearchScreenViewModel>()
    val repositories by viewModel.repositories.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val shouldShowTopBar by listState.isScrollingUp()

    val hasQueryBoxError = remember(viewModel.errorMessage.value) {
        mutableStateOf(viewModel.errorMessage.value != null)
    }

    val hapticFeedBack = getFeedbackOnLongPress()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.errorMessage.value) {
        if (viewModel.errorMessage.value != null) {
            val message = viewModel.errorMessage.value!!.error?.asString(context)
                ?: context.getString(UtilR.string.default_error)

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
                    Divider(
                        thickness = 1.dp,
                        color = LocalContentColor.current.onMediumEmphasis(0.4F),
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                    )
                }

                items(repositories) { repository ->
                    RepositoryCard(
                        repository = repository,
                        onClick = {
                            navigator.openRepositoryScreen(repository)
                        },
                        onLongClick = {
                            scope.launch {
                                hapticFeedBack()

                                clipboardManager.setText(
                                    AnnotatedString(repository.url)
                                )
                                snackbarHostState.showMessage(
                                    context.getString(UtilR.string.copied_link)
                                )
                            }
                        },
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                    )
                }
            }
        }
    }
}
