package com.flixclusive.feature.mobile.provider.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.provider.ProviderInstallationStatus
import com.flixclusive.core.navigation.navargs.ProviderMetadataNavArgs
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.components.material3.dialog.UnsafeInstallAlertDialog
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBar
import com.flixclusive.core.presentation.mobile.components.provider.ProviderCrashBottomSheet
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.extensions.showMessage
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.feature.mobile.provider.details.component.DescriptionBlock
import com.flixclusive.feature.mobile.provider.details.component.MainButtons
import com.flixclusive.feature.mobile.provider.details.component.NavigationItem
import com.flixclusive.feature.mobile.provider.details.component.ProviderDetailsHeader
import com.flixclusive.feature.mobile.provider.details.component.author.AuthorsList
import com.flixclusive.feature.mobile.provider.details.component.subdetails.SubDetailsList
import com.flixclusive.feature.mobile.provider.details.util.ProviderDetailsUiCommon.HORIZONTAL_PADDING
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Destination(navArgsDelegate = ProviderMetadataNavArgs::class)
@Composable
internal fun ProviderDetailsScreen(
    navigator: ProviderDetailsNavigator,
    args: ProviderMetadataNavArgs,
    viewModel: ProviderDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val warnOnInstall by viewModel.warnOnInstall.collectAsStateWithLifecycle()

    ProviderDetailsScreenContent(
        uiState = uiState,
        warnOnInstall = warnOnInstall,
        onGoBack = navigator::goBack,
        onToggleInstallation = viewModel::onToggleInstallation,
        onDisableInstallationWarning = viewModel::disableWarnOnInstall,
        onViewMarkdown = navigator::openMarkdownScreen,
        onConsumeInstallationError = viewModel::onConsumeInstallationError,
        onTestProviders = {
            if (uiState.installationStatus.isOutdated) {
                navigator.testProviders(arrayListOf(args.metadata))
            } else if (uiState.installationStatus.isInstalled) {
                navigator.testProviders(arrayListOf(uiState.metadata))
            }
        },
        onGoToProviderSettings = {
            if (uiState.installationStatus.isOutdated) {
                navigator.openProviderSettings(args.metadata)
            } else {
                navigator.openProviderSettings(uiState.metadata)
            }
        },
        onGoToRepository = {
            val repositoryUrl = if (uiState.installationStatus.isOutdated) {
                args.metadata.repositoryUrl
            } else {
                uiState.metadata.repositoryUrl
            }

            navigator.openAddProviderScreen(repositoryUrl.toValidRepositoryLink())
        },
    )
}

@Composable
private fun ProviderDetailsScreenContent(
    uiState: ProviderDetailsUiState,
    warnOnInstall: Boolean,
    onGoBack: () -> Unit,
    onGoToProviderSettings: () -> Unit,
    onGoToRepository: () -> Unit,
    onTestProviders: () -> Unit,
    onToggleInstallation: () -> Unit,
    onConsumeInstallationError: () -> Unit,
    onDisableInstallationWarning: (Boolean) -> Unit,
    onViewMarkdown: (String, String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val windowWidthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val metadata = uiState.metadata

    val webNavigationItems = remember {
        listOf(
            R.string.issue_a_bug to metadata.repositoryUrl.getNewIssueUrl(),
            R.string.browse_repository to metadata.repositoryUrl,
        )
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    var isWarnOnInstallDialogOpened by rememberSaveable { mutableStateOf(false) }

    Scaffold(
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
                                painter = painterResource(UiCommonR.drawable.provider_settings),
                                contentDescription = description,
                            )
                        }
                    }
                },
            )
        },
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(LocalGlobalScaffoldPadding.current),
    ) { innerPadding ->
        AnimatedContent(
            uiState.initializationError,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { state ->
            if (state != null) {
                RetryButton(
                    onRetry = onGoBack,
                    error = uiState.initializationError?.asString(),
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = innerPadding,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        ProviderDetailsHeader(
                            modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
                            provider = metadata,
                            installationStatus = uiState.installationStatus,
                            onToggleInstallationState = {
                                if (uiState.installationStatus.isNotInstalled && warnOnInstall) {
                                    isWarnOnInstallDialogOpened = true
                                    return@ProviderDetailsHeader
                                }

                                onToggleInstallation()
                            },
                            openRepositoryScreen = onGoToRepository,
                        )
                    }

                    item {
                        SubDetailsList(provider = metadata)
                    }

                    // Only show the big install/update button in portrait or compact mode
                    if (windowWidthSizeClass.isCompact) {
                        item {
                            MainButtons(
                                modifier = Modifier
                                    .padding(horizontal = HORIZONTAL_PADDING)
                                    .padding(bottom = 10.dp),
                                providerInstallationStatus = uiState.installationStatus,
                                onToggleInstallationState = {
                                    if (uiState.installationStatus.isNotInstalled && warnOnInstall) {
                                        isWarnOnInstallDialogOpened = true
                                        return@MainButtons
                                    }

                                    onToggleInstallation()
                                },
                            )
                        }
                    }

                    if (!uiState.installationStatus.isNotInstalled) {
                        item {
                            NavigationItem(
                                label = stringResource(id = LocaleR.string.run_tests),
                                onClick = onTestProviders,
                            )
                        }
                    }

                    if (metadata.changelog != null) {
                        item {
                            NavigationItem(
                                label = stringResource(id = LocaleR.string.whats_new),
                                onClick = {
                                    with(metadata) {
                                        if (changelog != null) {
                                            onViewMarkdown(name, changelog!!)
                                        } else {
                                            scope.launch {
                                                val message = context.getString(LocaleR.string.no_changelogs)
                                                snackbarHostState.showMessage(message)
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }

                    item {
                        DescriptionBlock(
                            description = metadata.description,
                            modifier =
                                Modifier
                                    .padding(horizontal = HORIZONTAL_PADDING),
                        )
                    }

                    item {
                        AuthorsList(
                            authors = metadata.authors,
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
        }

        if (uiState.installationError != null) {
            ProviderCrashBottomSheet(
                errors = listOf(uiState.installationError),
                onDismissRequest = onConsumeInstallationError,
                isLoading = false,
            )
        }
    }

    if (isWarnOnInstallDialogOpened) {
        UnsafeInstallAlertDialog(
            quantity = 1,
            formattedName = metadata.name,
            warnOnInstall = warnOnInstall,
            onConfirm = { disableWarning ->
                onDisableInstallationWarning(disableWarning)
                onToggleInstallation()
            },
            onDismiss = { isWarnOnInstallDialogOpened = false },
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
private fun ProviderDetailsScreenBasePreview() {
    val providerMetadata = DummyDataForPreview.getDummyProviderMetadata()

    FlixclusiveTheme {
        Surface {
            ProviderDetailsScreenContent(
                uiState = remember {
                    ProviderDetailsUiState(
                        metadata = providerMetadata,
                        installationStatus = ProviderInstallationStatus.Outdated,
                    )
                },
                warnOnInstall = true,
                onGoBack = {},
                onGoToProviderSettings = {},
                onGoToRepository = {},
                onTestProviders = {},
                onToggleInstallation = {},
                onConsumeInstallationError = {},
                onDisableInstallationWarning = {},
                onViewMarkdown = { _, _ -> },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ProviderDetailsScreenCompactLandscapePreview() {
    ProviderDetailsScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ProviderDetailsScreenMediumPortraitPreview() {
    ProviderDetailsScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ProviderDetailsScreenMediumLandscapePreview() {
    ProviderDetailsScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ProviderDetailsScreenExtendedPortraitPreview() {
    ProviderDetailsScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ProviderDetailsScreenExtendedLandscapePreview() {
    ProviderDetailsScreenBasePreview()
}
