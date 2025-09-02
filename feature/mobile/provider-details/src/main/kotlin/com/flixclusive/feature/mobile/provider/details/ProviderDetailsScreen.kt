package com.flixclusive.feature.mobile.provider.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.R
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.navigation.navargs.ProviderMetadataNavArgs
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.navigation.navigator.TestProvidersAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewMarkdownAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewProviderSettingsAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewRepositoryAction
import com.flixclusive.core.ui.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.components.dialog.UnsafeInstallAlertDialog
import com.flixclusive.core.presentation.mobile.components.provider.ProviderInstallationStatus
import com.flixclusive.core.presentation.mobile.components.topbar.CommonTopBar
import com.flixclusive.core.ui.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.ui.mobile.util.showMessage
import com.flixclusive.feature.mobile.provider.details.component.DescriptionBlock
import com.flixclusive.feature.mobile.provider.details.component.MainButtons
import com.flixclusive.feature.mobile.provider.details.component.NavigationItem
import com.flixclusive.feature.mobile.provider.details.component.ProviderDetailsHeader
import com.flixclusive.feature.mobile.provider.details.component.author.AuthorsList
import com.flixclusive.feature.mobile.provider.details.component.subdetails.SubDetailsList
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.provider.ProviderMetadata
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import com.flixclusive.core.strings.R as LocaleR

internal val HORIZONTAL_PADDING = 20.dp
internal const val LABEL_SIZE = 15
internal val LABEL_SIZE_IN_SP = LABEL_SIZE.sp
internal val LABEL_SIZE_IN_DP = LABEL_SIZE.dp
internal val SUB_LABEL_SIZE = 13.sp

interface ProviderDetailsNavigator :
    GoBackAction,
    ViewProviderSettingsAction,
    ViewRepositoryAction,
    TestProvidersAction,
    ViewMarkdownAction

// TODO: Refactor code + ViewModel
@Destination(
    navArgsDelegate = ProviderMetadataNavArgs::class,
)
@Composable
internal fun ProviderDetailsScreen(
    navigator: ProviderDetailsNavigator,
    args: ProviderMetadataNavArgs,
    viewModel: ProviderDetailsViewModel = hiltViewModel(),
) {
    with(viewModel) {
        ProviderDetailsScreen(
            args = args,
            providerPreferences = providerPreferences.collectAsStateWithLifecycle().value,
            providerMetadata = providerMetadata,
            snackbar = snackbar,
            installationStatus = providerInstallationStatus,
            onConsumeSnackbar = ::onConsumeSnackbar,
            onGoBack = navigator::goBack,
            onGoToProviderSettings = { navigator.openProviderSettings(args.providerMetadata) },
            onGoToRepository = { repository?.let(navigator::openRepositoryDetails) },
            onTestProviders = navigator::testProviders,
            onToggleInstallation = ::toggleInstallation,
            onDisableInstallationWarning = ::disableWarnOnInstall,
            onViewMarkdown = navigator::openMarkdownScreen,
        )
    }
}

@Composable
private fun ProviderDetailsScreen(
    args: ProviderMetadataNavArgs,
    providerPreferences: ProviderPreferences,
    providerMetadata: ProviderMetadata,
    snackbar: Resource.Failure?,
    installationStatus: ProviderInstallationStatus,
    onConsumeSnackbar: () -> Unit,
    onGoBack: () -> Unit,
    onGoToProviderSettings: () -> Unit,
    onGoToRepository: () -> Unit,
    onTestProviders: (ArrayList<ProviderMetadata>) -> Unit,
    onToggleInstallation: () -> Unit,
    onDisableInstallationWarning: (Boolean) -> Unit,
    onViewMarkdown: (String, String) -> Unit,
) {
    var openWarnOnInstallDialog by rememberSaveable { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val webNavigationItems =
        remember {
            listOf(
                LocaleR.string.issue_a_bug to providerMetadata.repositoryUrl.getNewIssueUrl(),
                LocaleR.string.browse_repository to providerMetadata.repositoryUrl,
            )
        }

    val onConsumeSnackbarUpdated by rememberUpdatedState(onConsumeSnackbar)
    LaunchedEffect(snackbar) {
        if (snackbar?.error != null) {
            snackbarHostState.showMessage(snackbar.error!!.asString(context))
            onConsumeSnackbarUpdated()
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(LocalGlobalScaffoldPadding.current),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CommonTopBar(
                title = "",
                onNavigate = onGoBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    val description = stringResource(LocaleR.string.provider_settings)
                    PlainTooltipBox(description) {
                        IconButton(onClick = onGoToProviderSettings) {
                            AdaptiveIcon(
                                painter = painterResource(R.drawable.provider_settings),
                                contentDescription = description,
                            )
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProviderDetailsHeader(
                    modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
                    providerMetadata = providerMetadata,
                    openRepositoryScreen = onGoToRepository,
                )
            }

            item {
                SubDetailsList(providerMetadata = providerMetadata)
            }

            item {
                MainButtons(
                    modifier =
                    Modifier
                        .padding(horizontal = HORIZONTAL_PADDING)
                        .padding(bottom = 10.dp),
                    providerInstallationStatus = installationStatus,
                    onTestProvider = {
                        if (installationStatus.isOutdated) {
                            onTestProviders(arrayListOf(args.providerMetadata))
                        } else if (installationStatus.isInstalled) {
                            onTestProviders(arrayListOf(providerMetadata))
                        }
                    },
                    onToggleInstallationState = {
                        if (installationStatus.isNotInstalled &&
                            providerPreferences.shouldWarnBeforeInstall
                        ) {
                            openWarnOnInstallDialog = true
                            return@MainButtons
                        }

                        onToggleInstallation()
                    },
                )
            }

            if (providerMetadata.changelog != null) {
                item {
                    NavigationItem(
                        label = stringResource(id = LocaleR.string.whats_new),
                        onClick = {
                            with(providerMetadata) {
                                if (changelog != null) {
                                    onViewMarkdown(name, changelog!!)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showMessage(
                                            context.getString(com.flixclusive.core.locale.R.string.no_changelogs),
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            }

            item {
                DescriptionBlock(
                    description = providerMetadata.description,
                    modifier =
                    Modifier
                        .padding(horizontal = HORIZONTAL_PADDING),
                )
            }

            item {
                AuthorsList(
                    authors = providerMetadata.authors,
                )
            }

            items(webNavigationItems) { (label, url) ->
                NavigationItem(
                    label = stringResource(id = label),
                    onClick = { uriHandler.openUri(url) },
                )
            }
        }
    }

    if (openWarnOnInstallDialog) {
        UnsafeInstallAlertDialog(
            quantity = 1,
            formattedName = providerMetadata.name,
            warnOnInstall = providerPreferences.shouldWarnBeforeInstall,
            onConfirm = { disableWarning ->
                onDisableInstallationWarning(disableWarning)
                onToggleInstallation()
            },
            onDismiss = { openWarnOnInstallDialog = false },
        )
    }
}

private fun String.getNewIssueUrl(): String {
    return if (contains("github.com")) {
        plus("/issues/new")
    } else {
        this
    }
}

@Preview
@Composable
private fun ProviderDetailsScreenPreview() {
    val providerMetadata = DummyDataForPreview.getDummyProviderMetadata()

    FlixclusiveTheme {
        Surface {
            ProviderDetailsScreen(
                args = ProviderMetadataNavArgs(providerMetadata),
                providerPreferences = ProviderPreferences(),
                providerMetadata = providerMetadata,
                snackbar = null,
                installationStatus = ProviderInstallationStatus.Installed,
                onConsumeSnackbar = {},
                onGoBack = {},
                onGoToProviderSettings = {},
                onGoToRepository = {},
                onTestProviders = {},
                onToggleInstallation = {},
                onDisableInstallationWarning = {},
                onViewMarkdown = { _, _ -> },
            )
        }
    }
}
