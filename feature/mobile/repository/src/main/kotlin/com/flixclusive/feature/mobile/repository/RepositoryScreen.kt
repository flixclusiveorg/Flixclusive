package com.flixclusive.feature.mobile.repository

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.dialog.IconAlertDialog
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.navargs.RepositoryScreenNavArgs
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.RetryButton
import com.flixclusive.core.ui.mobile.component.dialog.UnsafeInstallAlertDialog
import com.flixclusive.core.ui.mobile.component.provider.ButtonWithCircularProgressIndicator
import com.flixclusive.core.ui.mobile.component.provider.ProviderCard
import com.flixclusive.core.ui.mobile.component.provider.ProviderCardPlaceholder
import com.flixclusive.core.ui.mobile.component.provider.ProviderInstallationStatus
import com.flixclusive.core.ui.mobile.util.isAtTop
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.core.ui.mobile.util.showMessage
import com.flixclusive.feature.mobile.repository.component.RepositoryHeader
import com.flixclusive.feature.mobile.repository.component.RepositoryTopBar
import com.flixclusive.model.provider.ProviderData
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR


@OptIn(ExperimentalFoundationApi::class)
@Destination(
    navArgsDelegate = RepositoryScreenNavArgs::class
)
@Composable
internal fun RepositoryScreen(
    navigator: GoBackAction,
    args: RepositoryScreenNavArgs
) {
    val context = LocalContext.current

    val viewModel = hiltViewModel<RepositoryScreenViewModel>()

    val warnOnInstall by viewModel.warnOnInstall.collectAsStateWithLifecycle()
    val providerDataList by remember {
        derivedStateOf {
            when (viewModel.searchQuery.isNotEmpty()) {
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
    val listIsAtTop by listState.isAtTop()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val searchExpanded = rememberSaveable { mutableStateOf(false) }

    var providerToInstall by remember { mutableStateOf<ProviderData?>(null) }
    var providersToInstall by rememberSaveable { mutableIntStateOf(0) }
    var providerToUninstall by remember { mutableStateOf<ProviderData?>(null) }

    LaunchedEffect(viewModel.snackbar) {
        if (viewModel.snackbar?.error != null) {
            snackbarHostState.showMessage(viewModel.snackbar!!.error!!.asString(context))
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
    ) { innerPadding ->
        val topPadding by animateDpAsState(
            targetValue = if (listIsAtTop) innerPadding.calculateTopPadding() else 0.dp,
            label = ""
        )

        Box(
            modifier = Modifier
                .padding(top = topPadding)
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
                } else if (errorFetchingList) {
                    item {
                        RetryButton(
                            modifier = Modifier.fillMaxSize(),
                            shouldShowError = viewModel.uiState.error != null,
                            error = viewModel.uiState.error?.asString()
                                ?: stringResource(LocaleR.string.failed_to_load_online_providers),
                            onRetry = viewModel::initialize
                        )
                    }
                } else if (viewModel.onlineProviderMap.isNotEmpty()) {
                    item {
                        val canInstallAll by remember {
                            derivedStateOf {
                                viewModel.onlineProviderMap.any { (_, state) ->
                                    state.isNotInstalled
                                }
                            }
                        }

                        ButtonWithCircularProgressIndicator(
                            onClick = {
                                if (warnOnInstall) {
                                    providersToInstall = viewModel.onlineProviderMap.count { (_, state) ->
                                        state.isNotInstalled
                                    }
                                    return@ButtonWithCircularProgressIndicator
                                }

                                viewModel.installAll()
                            },
                            iconId = UiCommonR.drawable.download,
                            isLoading = viewModel.installAllJob?.isActive == true,
                            label = stringResource(id = LocaleR.string.install_all),
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
                            status = viewModel.onlineProviderMap[providerData]
                                ?: ProviderInstallationStatus.NotInstalled,
                            onClick = {
                                val isNotInstalled = viewModel.onlineProviderMap[providerData] !=
                                        ProviderInstallationStatus.Installed

                                if (isNotInstalled && warnOnInstall) {
                                    providerToInstall = providerData
                                } else if (isNotInstalled) {
                                    viewModel.toggleInstallationStatus(providerData)
                                } else {
                                    providerToUninstall = providerData
                                }
                            },
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .animateItem()
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    if (providerToInstall != null || providersToInstall > 0) {
        val quantity = when (providerToInstall) {
            null -> providersToInstall
            else -> 1
        }
        val formattedName: Any = if (quantity == 1 && providerToInstall == null) {
            viewModel.onlineProviderMap.firstNotNullOf {
                when (it.value) {
                    ProviderInstallationStatus.NotInstalled -> it.key
                    else -> null
                }
            }.name
        } else if (quantity == 1) providerToInstall!!.name
        else providersToInstall

        UnsafeInstallAlertDialog(
            quantity = quantity,
            formattedName = formattedName,
            warnOnInstall = warnOnInstall,
            onConfirm = { disableWarning ->
                if (providerToInstall == null) {
                    viewModel.installAll()
                    return@UnsafeInstallAlertDialog
                }

                viewModel.disableWarnOnInstall(disableWarning)
                viewModel.toggleInstallationStatus(providerToInstall!!)
            },
            onDismiss = {
                providerToInstall = null
                providersToInstall = 0
            }
        )
    }

    if (providerToUninstall != null) {
        IconAlertDialog(
            painter = painterResource(id = UiCommonR.drawable.warning),
            contentDescription = stringResource(id = LocaleR.string.warning_content_description),
            description = buildAnnotatedString {
                append(context.getString(LocaleR.string.warning_uninstall_message_first_half))
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(providerToUninstall!!.name)
                }
                append("?")
            },
            onConfirm = {
                viewModel.toggleInstallationStatus(providerToUninstall!!)
            },
            onDismiss = { providerToUninstall = null }
        )
    }
}