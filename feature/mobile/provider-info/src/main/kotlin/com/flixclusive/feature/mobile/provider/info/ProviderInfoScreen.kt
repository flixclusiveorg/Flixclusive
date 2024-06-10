package com.flixclusive.feature.mobile.provider.info

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.ProviderInfoScreenNavArgs
import com.flixclusive.core.ui.common.navigation.RepositorySearchScreenNavigator
import com.flixclusive.core.ui.common.util.DummyDataForPreview
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.ui.mobile.util.isAtTop
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.core.ui.mobile.util.showMessage
import com.flixclusive.feature.mobile.provider.info.component.DescriptionBlock
import com.flixclusive.feature.mobile.provider.info.component.MainButtons
import com.flixclusive.feature.mobile.provider.info.component.NavigationItem
import com.flixclusive.feature.mobile.provider.info.component.ProviderInfoHeader
import com.flixclusive.feature.mobile.provider.info.component.ProviderInfoTopBar
import com.flixclusive.feature.mobile.provider.info.component.author.AuthorsList
import com.flixclusive.feature.mobile.provider.info.component.subdetails.SubDetailsList
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.Repository
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.util.R as UtilR

internal val HORIZONTAL_PADDING = 20.dp
internal const val LABEL_SIZE = 15
internal val LABEL_SIZE_IN_SP = LABEL_SIZE.sp
internal val LABEL_SIZE_IN_DP = LABEL_SIZE.dp
internal val SUB_LABEL_SIZE = 13.sp

interface ProviderInfoNavigator : RepositorySearchScreenNavigator {
    fun testProviders(providers: List<ProviderData>)
    fun seeWhatsNew(providerData: ProviderData)
    fun openProviderSettings(providerData: ProviderData)
}

@Destination(
    navArgsDelegate = ProviderInfoScreenNavArgs::class
)
@Composable
fun ProviderInfoScreen(
    navigator: ProviderInfoNavigator,
    args: ProviderInfoScreenNavArgs
) {
    val viewModel = hiltViewModel<ProviderInfoScreenViewModel>()

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val listState = rememberLazyListState()
    val shouldShowTopBar by listState.isScrollingUp()
    val listIsAtTop by listState.isAtTop()

    val snackbarHostState = remember { SnackbarHostState() }

    val webNavigationItems = remember {
        listOf(
            UtilR.string.issue_a_bug to args.providerData.repositoryUrl?.getNewIssueUrl(),
            UtilR.string.browse_repository to args.providerData.repositoryUrl,
        )
    }

    LaunchedEffect(viewModel.snackbar) {
        if (viewModel.snackbar?.error != null) {
            snackbarHostState.showMessage(viewModel.snackbar!!.error!!.asString(context))
            viewModel.onConsumeSnackbar()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ProviderInfoTopBar(
                isVisible = shouldShowTopBar,
                providerName = args.providerData.name,
                onNavigationIconClick = navigator::goBack,
                onSettingsClick = {
                    navigator.openProviderSettings(args.providerData)
                }
            )
        }
    ) { innerPadding ->
        val surface = MaterialTheme.colorScheme.surface
        val emphasizedBackgroundColor = MaterialTheme.colorScheme.primary
        val topPadding by animateDpAsState(
            targetValue = if (listIsAtTop) innerPadding.calculateTopPadding() else 0.dp,
            label = ""
        )

        Box(
            modifier = Modifier
                .padding(top = topPadding)
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(emphasizedBackgroundColor, surface),
                            center = Offset(
                                x = size.width,
                                y = size.height
                            ),
                            radius = size.width.times(0.85F)
                        )
                    )

                    drawRect(surface.copy(alpha = 0.7F))
                }
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ProviderInfoHeader(
                        modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
                        providerData = args.providerData,
                        openRepositoryScreen = {
                            viewModel.repository?.let(navigator::openRepositoryScreen)
                        }
                    )
                }

                item {
                    SubDetailsList(providerData = args.providerData)
                }

                item {
                    MainButtons(
                        modifier = Modifier
                            .padding(horizontal = HORIZONTAL_PADDING)
                            .padding(bottom = 10.dp),
                        providerInstallationStatus = viewModel.providerInstallationStatus,
                        onTestProvider = {
                            navigator.testProviders(listOf(args.providerData))
                        },
                        onToggleInstallationState = viewModel::toggleInstallation
                    )
                }

                if (args.providerData.changelog != null) {
                    item {
                        NavigationItem(
                            label = stringResource(id = UtilR.string.whats_new),
                            onClick = {
                                navigator.seeWhatsNew(providerData = args.providerData)
                            }
                        )
                    }
                }

                item {
                    DescriptionBlock(
                        description = args.providerData.description,
                        modifier = Modifier
                            .padding(horizontal = HORIZONTAL_PADDING)
                    )
                }

                item {
                    AuthorsList(
                        authors = args.providerData.authors,
                    )
                }

                items(webNavigationItems) { (label, url) ->
                    NavigationItem(
                        label = stringResource(id = label),
                        onClick = {
                            if (url != null) uriHandler.openUri(url)
                            else {
                                context.showToast(context.getString(UtilR.string.null_repository_url_error))
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun String.getNewIssueUrl(): String {
    return if (contains("github.com"))
        plus("/issues/new")
    else this
}

@Preview
@Composable
private fun ProviderInfoScreenPreview() {
    val providerData = DummyDataForPreview.getDummyProviderData()

    FlixclusiveTheme {
        Surface {
            ProviderInfoScreen(
                navigator = object : ProviderInfoNavigator {
                    override fun goBack() {}
                    override fun testProviders(providers: List<ProviderData>) {}
                    override fun seeWhatsNew(providerData: ProviderData) {}
                    override fun openProviderSettings(providerData: ProviderData) {}
                    override fun openRepositoryScreen(repository: Repository) {}
                },
                args = ProviderInfoScreenNavArgs(
                    providerData = providerData
                )
            )
        }
    }
}