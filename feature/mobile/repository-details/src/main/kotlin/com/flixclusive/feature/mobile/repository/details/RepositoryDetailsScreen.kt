package com.flixclusive.feature.mobile.repository.details

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.dialog.IconAlertDialog
import com.flixclusive.core.ui.common.navigation.navargs.RepositoryScreenNavArgs
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.util.DummyDataForPreview
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.RetryButton
import com.flixclusive.core.ui.mobile.component.dialog.UnsafeInstallAlertDialog
import com.flixclusive.core.ui.mobile.component.provider.ButtonWithCircularProgressIndicator
import com.flixclusive.core.ui.mobile.component.provider.ProviderCard
import com.flixclusive.core.ui.mobile.component.provider.ProviderCardPlaceholder
import com.flixclusive.core.ui.mobile.component.provider.ProviderInstallationStatus
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarWithSearch
import com.flixclusive.core.ui.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.ui.mobile.util.showMessage
import com.flixclusive.feature.mobile.repository.details.component.RepositoryHeader
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

// TODO: Refactor code + ViewModel
@OptIn(ExperimentalFoundationApi::class)
@Destination(
    navArgsDelegate = RepositoryScreenNavArgs::class,
)
@Composable
internal fun RepositoryDetailsScreen(
    navigator: GoBackAction,
    args: RepositoryScreenNavArgs,
    viewModel: RepositoryDetailsViewModel = hiltViewModel(),
) {
    val warnOnInstall by viewModel.warnOnInstall.collectAsStateWithLifecycle()

    RepositoryDetailsScreen(
        warnOnInstall = warnOnInstall,
        onlineProviderMap = viewModel.onlineProviderMap,
        uiState = viewModel.uiState,
        resourceError = viewModel.snackbar,
        repository = args.repository,
        isInstalling = viewModel.installAllJob?.isActive == true,
        searchQuery = { viewModel.searchQuery },
        onConsumeSnackbar = viewModel::onConsumeSnackbar,
        onGoBack = navigator::goBack,
        onRetry = viewModel::initialize,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onToggleInstallationStatus = viewModel::toggleInstallationStatus,
        onInstallAll = viewModel::installAll,
        onDisableInstallationWarning = viewModel::disableWarnOnInstall
    )
}

@Composable
private fun RepositoryDetailsScreen(
    warnOnInstall: Boolean,
    onlineProviderMap: Map<ProviderMetadata, ProviderInstallationStatus>,
    uiState: Resource<List<ProviderMetadata>>,
    resourceError: Resource.Failure?,
    repository: Repository,
    isInstalling: Boolean,
    searchQuery: () -> String,
    onConsumeSnackbar: () -> Unit,
    onGoBack: () -> Unit,
    onRetry: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleInstallationStatus: (ProviderMetadata) -> Unit,
    onInstallAll: () -> Unit,
    onDisableInstallationWarning: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    val providerMetadataList by remember {
        derivedStateOf {
            when (searchQuery().isNotEmpty()) {
                true -> {
                    onlineProviderMap.keys
                        .toList()
                        .fastFilter { it.name.contains(searchQuery(), true) }
                }

                false -> onlineProviderMap.keys.toList()
            }
        }
    }

    val errorFetchingList =
        remember(onlineProviderMap, uiState) {
            onlineProviderMap.isEmpty() && uiState.error != null
        }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }

    var providerToInstall by remember { mutableStateOf<ProviderMetadata?>(null) }
    var providersToInstall by rememberSaveable { mutableIntStateOf(0) }
    var providerToUninstall by remember { mutableStateOf<ProviderMetadata?>(null) }

    val onConsumeSnackbarUpdated by rememberUpdatedState(onConsumeSnackbar)
    LaunchedEffect(resourceError) {
        if (resourceError?.error != null) {
            snackbarHostState.showMessage(resourceError.error!!.asString(context))
            onConsumeSnackbarUpdated()
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier =
            Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(LocalGlobalScaffoldPadding.current),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            CommonTopBarWithSearch(
                isSearching = isSearching,
                searchQuery = searchQuery,
                onQueryChange = onSearchQueryChange,
                title = stringResource(LocaleR.string.repository),
                scrollBehavior = scrollBehavior,
                onNavigate = onGoBack,
                onToggleSearchBar = { isSearching = it },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = innerPadding,
            modifier =
                Modifier
                    .padding(horizontal = 10.dp),
        ) {
            item {
                RepositoryHeader(
                    repository = repository,
                    toggleSnackbar = {
                        scope.launch {
                            snackbarHostState.showMessage(it.asString(context))
                        }
                    },
                )
            }

            item {
                HorizontalDivider(
                    modifier =
                        Modifier
                            .padding(vertical = 10.dp),
                    thickness = 1.dp,
                    color = LocalContentColor.current.onMediumEmphasis(0.4F),
                )
            }

            if (uiState.isLoading) {
                items(4) {
                    ProviderCardPlaceholder()
                }
            } else if (errorFetchingList) {
                item {
                    RetryButton(
                        modifier = Modifier.fillMaxSize(),
                        shouldShowError = uiState.error != null,
                        error =
                            uiState.error?.asString()
                                ?: stringResource(LocaleR.string.failed_to_load_online_providers),
                        onRetry = onRetry,
                    )
                }
            } else if (onlineProviderMap.isNotEmpty()) {
                item {
                    val canInstallAll by remember {
                        derivedStateOf {
                            onlineProviderMap.any { (_, state) ->
                                state.isNotInstalled
                            }
                        }
                    }

                    ButtonWithCircularProgressIndicator(
                        onClick = {
                            if (warnOnInstall) {
                                providersToInstall =
                                    onlineProviderMap.count { (_, state) ->
                                        state.isNotInstalled
                                    }
                                return@ButtonWithCircularProgressIndicator
                            }

                            onInstallAll()
                        },
                        iconId = UiCommonR.drawable.download,
                        isLoading = isInstalling,
                        label = stringResource(id = LocaleR.string.install_all),
                        enabled = canInstallAll,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                items(
                    providerMetadataList,
                    key = { item -> "${item.id}-${item.buildUrl}" },
                ) { providerMetadata ->
                    ProviderCard(
                        providerMetadata = providerMetadata,
                        status =
                            onlineProviderMap[providerMetadata]
                                ?: ProviderInstallationStatus.NotInstalled,
                        onClick = {
                            val isNotInstalled =
                                onlineProviderMap[providerMetadata] !=
                                    ProviderInstallationStatus.Installed

                            if (isNotInstalled && warnOnInstall) {
                                providerToInstall = providerMetadata
                            } else if (isNotInstalled) {
                                onToggleInstallationStatus(providerMetadata)
                            } else {
                                providerToUninstall = providerMetadata
                            }
                        },
                        modifier =
                            Modifier
                                .padding(vertical = 5.dp)
                                .animateItem(),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (providerToInstall != null || providersToInstall > 0) {
        val quantity =
            when (providerToInstall) {
                null -> providersToInstall
                else -> 1
            }
        val formattedName: Any =
            if (quantity == 1 && providerToInstall == null) {
                onlineProviderMap
                    .firstNotNullOf {
                        when (it.value) {
                            ProviderInstallationStatus.NotInstalled -> it.key
                            else -> null
                        }
                    }.name
            } else if (quantity == 1) {
                providerToInstall!!.name
            } else {
                providersToInstall
            }

        UnsafeInstallAlertDialog(
            quantity = quantity,
            formattedName = formattedName,
            warnOnInstall = warnOnInstall,
            onConfirm = { disableWarning ->
                if (providerToInstall == null) {
                    onInstallAll()
                    return@UnsafeInstallAlertDialog
                }

                onDisableInstallationWarning(disableWarning)
                onToggleInstallationStatus(providerToInstall!!)
            },
            onDismiss = {
                providerToInstall = null
                providersToInstall = 0
            },
        )
    }

    if (providerToUninstall != null) {
        IconAlertDialog(
            painter = painterResource(id = UiCommonR.drawable.warning),
            contentDescription = stringResource(id = LocaleR.string.warning_content_description),
            description =
                buildAnnotatedString {
                    append(context.getString(LocaleR.string.warning_uninstall_message_first_half))
                    append(" ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(providerToUninstall!!.name)
                    }
                    append("?")
                },
            onConfirm = {
                onToggleInstallationStatus(providerToUninstall!!)
            },
            onDismiss = { providerToUninstall = null },
        )
    }
}

@Preview
@Composable
private fun RepositoryDetailsScreenBasePreview() {
    val providerMetadata = DummyDataForPreview.getDummyProviderMetadata()
    val args = remember {
        RepositoryScreenNavArgs(
            Repository(
                owner = "flixclusiveorg",
                name = "provider-template",
                url = "",
                rawLinkFormat = ""
            )
        )
    }
    val list = remember {
        List(10) {
            providerMetadata.copy(id = it.toString())
        }
    }
    val map = remember {
        List(10) {
            providerMetadata.copy(id = it.toString()) to ProviderInstallationStatus.NotInstalled
        }.toMap()
    }

    FlixclusiveTheme {
        Surface {
            RepositoryDetailsScreen(
                repository = args.repository,
                onConsumeSnackbar = {},
                onGoBack = {},
                onDisableInstallationWarning = {},
                warnOnInstall = false,
                onlineProviderMap = map,
                uiState = Resource.Success(list),
                resourceError = null,
                isInstalling = false,
                searchQuery = { "" },
                onRetry = {},
                onInstallAll = {},
                onSearchQueryChange = {},
                onToggleInstallationStatus = {}
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun RepositoryDetailsScreenCompactLandscapePreview() {
    RepositoryDetailsScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun RepositoryDetailsScreenMediumPortraitPreview() {
    RepositoryDetailsScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun RepositoryDetailsScreenMediumLandscapePreview() {
    RepositoryDetailsScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun RepositoryDetailsScreenExtendedPortraitPreview() {
    RepositoryDetailsScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun RepositoryDetailsScreenExtendedLandscapePreview() {
    RepositoryDetailsScreenBasePreview()
}
