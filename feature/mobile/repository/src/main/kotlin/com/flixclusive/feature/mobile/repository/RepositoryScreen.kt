package com.flixclusive.feature.mobile.repository

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.RetryButton
import com.flixclusive.core.ui.mobile.component.provider.ProviderCard
import com.flixclusive.core.ui.mobile.component.provider.ProviderCardPlaceholder
import com.flixclusive.core.ui.mobile.component.provider.ProviderCardState
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.core.ui.mobile.util.showMessage
import com.flixclusive.feature.mobile.repository.component.CustomOutlineButton
import com.flixclusive.feature.mobile.repository.component.RepositoryHeader
import com.flixclusive.feature.mobile.repository.component.RepositoryTopBar
import com.flixclusive.gradle.entities.Repository
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

data class RepositoryScreenNavArgs(
    val repository: Repository
)

@OptIn(ExperimentalFoundationApi::class)
@Destination(
    navArgsDelegate = RepositoryScreenNavArgs::class
)
@Composable
fun RepositoryScreen(
    navigator: GoBackAction,
    args: RepositoryScreenNavArgs
) {
    val context = LocalContext.current

    val viewModel = hiltViewModel<RepositoryScreenViewModel>()
    val providerDataList by remember {
        derivedStateOf {
            when(viewModel.searchQuery.isNotEmpty()) {
                true -> {
                    viewModel.onlineProviderMap.keys.toList()
                        .fastFilter {
                            it.name.contains(viewModel.searchQuery, true)
                        }
                }
                false -> viewModel.onlineProviderMap.keys.toList()
            }
        }
    }

    val errorFetchingList = remember(viewModel.onlineProviderMap, viewModel.uiState) {
        viewModel.onlineProviderMap.isEmpty() && viewModel.uiState.error != null
    }

    val listState = rememberLazyListState()
    val shouldShowTopBar by listState.isScrollingUp()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val searchExpanded = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel.snackbarError) {
        if (viewModel.snackbarError?.error != null) {
            snackbarHostState.showMessage(viewModel.snackbarError!!.error!!.asString(context))
            viewModel.onConsumeSnackbar()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            RepositoryTopBar(
                isVisible = shouldShowTopBar,
                searchExpanded = searchExpanded,
                searchQuery = viewModel.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
            ) {
                item {
                    RepositoryHeader(
                        repository = args.repository,
                        toggleSnackbar = {
                            scope.launch {
                                snackbarHostState.showMessage(it.asString(context))
                            }
                        }
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

                if (viewModel.uiState.isLoading) {
                    items(4) {
                        ProviderCardPlaceholder()
                    }
                }
                else if (errorFetchingList) {
                    item {
                        RetryButton(
                            modifier = Modifier.fillMaxSize(),
                            shouldShowError = viewModel.uiState.error != null,
                            error = viewModel.uiState.error?.asString() ?: stringResource(UtilR.string.failed_to_load_online_providers),
                            onRetry = viewModel::initialize
                        )
                    }
                }
                else if (viewModel.onlineProviderMap.isNotEmpty()) {
                    item {
                        val canInstallAll by remember {
                            derivedStateOf {
                                viewModel.onlineProviderMap.any { (_, state) ->
                                    state == ProviderCardState.NotInstalled
                                }
                            }
                        }

                        CustomOutlineButton(
                            onClick = viewModel::installAll,
                            iconId = UiCommonR.drawable.download,
                            isLoading = viewModel.installAllJob?.isActive == true,
                            label = stringResource(id = UtilR.string.install_all),
                            enabled = canInstallAll,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    items(
                        providerDataList,
                        key = { item -> "${item.name}-${item.buildUrl}" }
                    ) { providerData ->
                        ProviderCard(
                            providerData = providerData,
                            state = viewModel.onlineProviderMap[providerData] ?: ProviderCardState.NotInstalled,
                            onClick = { viewModel.toggleProvider(providerData) },
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .animateItemPlacement()
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}