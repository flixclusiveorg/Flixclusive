package com.flixclusive.feature.mobile.provider.details

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.navigation.navargs.ProviderMetadataNavArgs
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.material3.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.components.provider.ProviderInstallButton
import com.flixclusive.core.presentation.mobile.components.provider.ProviderInstallState
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.feature.mobile.provider.details.component.AuthorCard
import com.flixclusive.feature.mobile.provider.details.component.CapabilitiesSection
import com.flixclusive.feature.mobile.provider.details.component.NavigationItem
import com.flixclusive.feature.mobile.provider.details.component.ProviderDetailsDescription
import com.flixclusive.feature.mobile.provider.details.component.ProviderDetailsDivider
import com.flixclusive.feature.mobile.provider.details.component.ProviderDetailsExtraChips
import com.flixclusive.feature.mobile.provider.details.component.ProviderDetailsHeader
import com.flixclusive.feature.mobile.provider.details.component.ProviderDetailsWhatsNew
import com.flixclusive.feature.mobile.provider.details.component.SectionLabel
import com.flixclusive.feature.mobile.provider.details.component.UnsafeInstallAlertDialog
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.spec.DestinationStyle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Destination<ExternalModuleGraph>(
    navArgs = ProviderMetadataNavArgs::class,
    style = DestinationStyle.Dialog::class,
)
@Composable
internal fun ProviderDetailsBottomSheet(
    args: ProviderMetadataNavArgs,
    navigator: NavigatorProviderDetailsBottomSheet,
    viewModel: ProviderDetailsBottomSheetViewModel = hiltViewModel()
) {
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }

    val warnOnInstall by viewModel.warnOnInstall.collectAsStateWithLifecycle()
    val installState by viewModel.installState.collectAsStateWithLifecycle()
    val capabilities by viewModel.capabilities.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.errors
            .collect {
                snackbarHostState.showSnackbar(
                    message = it.asString(resources)
                )
            }
    }

    ProviderDetailsBottomSheetContent(
        provider = args.metadata,
        warnOnInstall = warnOnInstall,
        snackbarHostState = snackbarHostState,
        onToggleInstallState = viewModel::onToggleInstallState,
        onBack = navigator::navigateBack,
        onDisableInstallationWarning = viewModel::onDisableInstallationWarning,
        onForceUninstall = viewModel::onUninstall,
        installState = { installState },
        capabilities = { capabilities },
        onToggleCapability = viewModel::toggleCapability,
        onConfigure = {
            navigator.navigateToProviderSettings(provider = args.metadata)
        },
        onRepositoryClick = {
            navigator.navigateToAddProviderScreen(
                initialSelectedRepositoryFilter = safeCall {
                    args.metadata.repositoryUrl.toValidRepositoryLink()
                }
            )
        },
        onViewChangeLogs = {
            navigator.navigateToMarkdownScreen(
                title = args.metadata.name,
                description = args.metadata.changelog ?: resources.getString(LocaleR.string.no_changelogs)
            )
        },
    )
}

private fun String.getNewIssueUrl(): String {
    return if (contains("github.com")) {
        plus("/issues/new")
    } else {
        this
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun ProviderDetailsBottomSheetContent(
    provider: ProviderMetadata,
    warnOnInstall: Boolean,
    installState: () -> ProviderInstallState,
    onRepositoryClick: () -> Unit,
    onViewChangeLogs: () -> Unit,
    onToggleInstallState: () -> Unit,
    onBack: () -> Unit,
    onConfigure: () -> Unit,
    onForceUninstall: () -> Unit,
    onDisableInstallationWarning: (Boolean) -> Unit,
    capabilities: () -> List<CapabilityItem>,
    onToggleCapability: (ProviderCapability) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uriHandler = LocalUriHandler.current
    val sheetState = rememberModalBottomSheetState()

    var isWarnOnInstallDialogOpened by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val providerDescription = provider.description
    val providerLatestChanges by remember {
        derivedStateOf {
            val state = installState()
            if (state is ProviderInstallState.Outdated) {
                state.newChangelogs to state.newVersion
            } else {
                provider.changelog to provider.versionName
            }
        }
    }

    val webNavigationItems = remember {
        listOf(
            Triple(
                R.string.issue_a_bug,
                provider.repositoryUrl.getNewIssueUrl(),
                UiCommonR.drawable.bug_thin
            ),
            Triple(
                R.string.browse_repository,
                provider.repositoryUrl,
                UiCommonR.drawable.github_outline
            )
        )
    }

    CommonBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onBack,
        modifier = modifier,
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .offset {
                            val offsetY = runCatching {
                                sheetState.requireOffset().roundToInt()
                            }.getOrDefault(0)

                            IntOffset(x = 0, y = -offsetY)
                        }
                )
            },
            modifier = Modifier
        ) {
            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp
                ),
            ) {
                item {
                    ProviderDetailsHeader(
                        provider = provider,
                        onRepositoryClick = onRepositoryClick
                    )
                }

                item {
                    ProviderDetailsExtraChips(
                        provider = provider,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                    )
                }

                item {
                    ProviderInstallButton(
                        state = installState,
                        onUninstall = onForceUninstall,
                        onConfigure = onConfigure,
                        onToggleInstallState = {
                            if (installState() is ProviderInstallState.NotInstalled) {
                                isWarnOnInstallDialogOpened = true
                                return@ProviderInstallButton
                            }

                            onToggleInstallState()
                        },
                    )
                }

                item {
                    ProviderDetailsDivider(
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                if (providerDescription?.isNotBlank() == true) {
                    item {
                        ProviderDetailsDescription(
                            description = providerDescription
                        )
                    }

                    item {
                        ProviderDetailsDivider(
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }

                if (capabilities().isNotEmpty()) {
                    item {
                        CapabilitiesSection(
                            installState = installState,
                            capabilities = capabilities,
                            onToggleCapability = onToggleCapability,
                        )
                    }
                }

                providerLatestChanges.first?.let { changelogs ->
                    item {
                        ProviderDetailsWhatsNew(
                            changelogs = changelogs,
                            latestVersion = providerLatestChanges.second,
                            onViewChangelogs = onViewChangeLogs,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        ProviderDetailsDivider(
                            modifier = Modifier.padding(top = 10.dp, bottom = 5.dp)
                        )
                    }
                }

                itemsIndexed(webNavigationItems) { i, (label, url, icon) ->
                    Column {
                        NavigationItem(
                            label = stringResource(id = label),
                            icon = painterResource(id = icon),
                            onClick = { uriHandler.openUri(url) },
                        )

                        if (i < webNavigationItems.size - 1) {
                            ProviderDetailsDivider(
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                    }
                }

                if (provider.authors.isNotEmpty()) {
                    item {
                        ProviderDetailsDivider(
                            modifier = Modifier.padding(
                                bottom = 10.dp,
                                top = 5.dp
                            )
                        )
                    }

                    item {
                        SectionLabel(
                            text = stringResource(id = LocaleR.string.authors),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )
                    }

                    items(provider.authors) { author ->
                        AuthorCard(
                            author = author,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (isWarnOnInstallDialogOpened) {
            UnsafeInstallAlertDialog(
                quantity = 1,
                formattedName = provider.name,
                warnOnInstall = warnOnInstall,
                onDismiss = { isWarnOnInstallDialogOpened = false },
                onConfirm = { disableWarning ->
                    onDisableInstallationWarning(disableWarning)
                    onToggleInstallState()
                },
                modifier = Modifier.padding(horizontal = 25.dp)
            )
        }
    }
}

@Preview
@Composable
private fun ProviderDetailsBottomSheetPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(true) {
        delay(800)
        snackbarHostState.showSnackbar(
            message = "This is a snackbar message"
        )
    }
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.background(
                    Color.Black.copy(alpha = 0.5f)
                )
            )

            ProviderDetailsBottomSheetContent(
                provider = DummyDataForPreview.getProviderMetadata(
                    language = Language(code = "en")
                ),
                warnOnInstall = true,
                installState = { ProviderInstallState.NotInstalled },
                onRepositoryClick = {},
                onViewChangeLogs = {},
                onToggleInstallState = {},
                onBack = {},
                snackbarHostState = snackbarHostState,
                onDisableInstallationWarning = {},
                onConfigure = {},
                onForceUninstall = {},
                onToggleCapability = {},
                capabilities = {
                    buildList {
                        add(
                            CapabilityItem(
                                capability = ProviderCapability.CROSS_MATCH,
                                isEnabled = true,
                                description = UiText.from("Sample description for cross match capability."),
                                label = UiText.from("Cross Match")
                            )
                        )
                    }
                }
            )
        }
    }
}
