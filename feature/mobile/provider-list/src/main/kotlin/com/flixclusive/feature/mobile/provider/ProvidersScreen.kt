package com.flixclusive.feature.mobile.provider

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFilter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.R
import com.flixclusive.core.ui.common.dialog.IconAlertDialog
import com.flixclusive.core.ui.common.dialog.TextAlertDialog
import com.flixclusive.core.ui.common.navigation.navigator.ProvidersScreenNavigator
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.ui.mobile.component.EmptyDataMessage
import com.flixclusive.core.ui.mobile.component.provider.InstalledProviderCard
import com.flixclusive.core.ui.mobile.util.getFeedbackOnLongPress
import com.flixclusive.core.ui.mobile.util.isAtTop
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.data.provider.util.getApiCrashMessage
import com.flixclusive.data.provider.util.isNotUsable
import com.flixclusive.feature.mobile.provider.component.CustomButton
import com.flixclusive.feature.mobile.provider.component.ProfileHandlerButtons
import com.flixclusive.feature.mobile.provider.component.ProvidersTopBar
import com.flixclusive.feature.mobile.provider.util.DragAndDropUtils.dragGestureHandler
import com.flixclusive.feature.mobile.provider.util.rememberDragDropListState
import com.flixclusive.model.provider.ProviderMetadata
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

private val FabButtonSize = 56.dp
private fun Context.getHelpGuideTexts()
    = resources.getStringArray(LocaleR.array.providers_screen_help)

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
internal fun ProvidersScreen(
    navigator: ProvidersScreenNavigator
) {
    val context = LocalContext.current

    val viewModel = hiltViewModel<ProvidersScreenViewModel>()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val providerToggles by viewModel.providerPrefs.collectAsStateWithLifecycle()
    val userOnBoardingPrefs by viewModel.userOnBoardingPrefs.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle(null)
    val searchExpanded = rememberSaveable { mutableStateOf(false) }
    var providerToUninstall by rememberSaveable { mutableStateOf<ProviderMetadata?>(null) }

    val helpTooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val overscrollJob = remember { mutableStateOf<Job?>(null) }
    val dragDropListState = rememberDragDropListState(
        onMove = { fromIndex, toIndex ->
            if (!searchExpanded.value) {
                // -1 since there's a header
                viewModel.onMove(fromIndex - 1, toIndex - 1)
            }
        }
    )
    val listState = dragDropListState.getLazyListState()
    val shouldShowTopBar by listState.isScrollingUp()
    val listIsAtTop by listState.isAtTop()

    LaunchedEffect(error) {
        if (error == null) return@LaunchedEffect

        val faultyProvider = providers.find { it.id == error!!.providerId } ?: return@LaunchedEffect
        val message = context.getApiCrashMessage(faultyProvider.name)

        context.showToast(message)
    }

    val filteredProviders by remember {
        derivedStateOf {
            when (viewModel.searchQuery.isNotEmpty() && searchExpanded.value) {
                true -> providers.fastFilter { it.name.contains(viewModel.searchQuery, true) }
                false -> null
            }
        }
    }

    val featureComingSoonCallback = {
        context.showToast(context.getString(LocaleR.string.coming_soon_feature))
    }

    val onNeedHelp = {
        val (title, description) = context.getHelpGuideTexts()
        navigator.openMarkdownScreen(
            title = title,
            description = description
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ProvidersTopBar(
                isVisible = shouldShowTopBar,
                searchQuery = viewModel.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                tooltipState = helpTooltipState,
                searchExpanded = searchExpanded,
                onNeedHelp = onNeedHelp
            )
        },
        floatingActionButton = {
            if (providers.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = navigator::openAddRepositoryScreen,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    expanded = !shouldShowTopBar,
                    text = {
                        Text(text = stringResource(LocaleR.string.add_provider))
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = UiCommonR.drawable.round_add_24),
                            contentDescription = stringResource(LocaleR.string.add_provider)
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        val topPadding by animateDpAsState(
            targetValue = if (listIsAtTop) innerPadding.calculateTopPadding() else 0.dp,
            label = ""
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding)
        ) {
            AnimatedContent(
                targetState = providers.isEmpty(),
                label = "",
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = fadeIn(),
                        initialContentExit = fadeOut()
                    )
                }
            ) { state ->
                if (state) {
                    EmptyDataMessage(
                        modifier = Modifier.fillMaxSize(),
                        description = stringResource(LocaleR.string.empty_providers_list_message),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                        ) {
                            MissingProvidersLogo()
                            OutlinedButton(
                                onClick = navigator::openAddRepositoryScreen,
                                modifier = Modifier
                            ) {
                                Text(text = stringResource(LocaleR.string.add_provider))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            bottom = FabButtonSize * 2,
                        ),
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .dragGestureHandler(
                                scope = scope,
                                itemListDragAndDropState = dragDropListState,
                                overscrollJob = overscrollJob,
                                feedbackLongPress = getFeedbackOnLongPress()
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            AnimatedVisibility(
                                visible = !searchExpanded.value,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ProfileHandlerButtons(
                                        modifier = Modifier.padding(top = 15.dp),
                                        onImport = featureComingSoonCallback,
                                        onExport = featureComingSoonCallback
                                    )

                                    CustomButton(
                                        onClick = {
                                            navigator.testProviders(
                                                providers = providers.toCollection(ArrayList())
                                            )
                                        },
                                        iconId = UiCommonR.drawable.test,
                                        label = stringResource(id = LocaleR.string.test_providers),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 3.dp)
                                    )

                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = LocalContentColor.current.onMediumEmphasis(0.4F)
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = filteredProviders ?: providers,
                            key = { _, item -> item.id }
                        ) { index, metadata ->
                            val displacementOffset =
                                // +1 since there's a header
                                if (index + 1 == dragDropListState.getCurrentIndexOfDraggedListItem()) {
                                    dragDropListState.elementDisplacement.takeIf { it != 0f }
                                } else null

                            val isDisabled = providerToggles.getOrNull(index)
                            val isEnabled = !metadata.isNotUsable && isDisabled == false

                            InstalledProviderCard(
                                providerMetadata = metadata,
                                enabled = isEnabled,
                                isDraggable = !searchExpanded.value,
                                displacementOffset = displacementOffset,
                                openSettings = { navigator.openProviderSettings(metadata) },
                                onClick = { navigator.openProviderInfo(metadata) },
                                uninstallProvider = { providerToUninstall = metadata },
                                onToggleProvider = {
                                    viewModel.toggleProvider(
                                        id = metadata.id,
                                        isEnabled = isEnabled
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (providerToUninstall != null) {
        val metadata = remember { providerToUninstall!! }

        IconAlertDialog(
            painter = painterResource(id = R.drawable.warning),
            contentDescription = stringResource(id = LocaleR.string.warning_content_description),
            description = buildAnnotatedString {
                append(context.getString(LocaleR.string.warning_uninstall_message_first_half))
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(metadata.name)
                }
                append("?")
            },
            onConfirm = {
                viewModel.uninstallProvider(metadata)
                providerToUninstall = null
            },
            onDismiss = { providerToUninstall = null }
        )
    }

    if (userOnBoardingPrefs.isFirstTimeOnProvidersScreen) {
        TextAlertDialog(
            label = stringResource(LocaleR.string.first_time_providers_screen_title),
            description = stringResource(LocaleR.string.first_time_providers_screen_message),
            dismissButtonLabel = stringResource(id = LocaleR.string.skip),
            dismissOnConfirm = false,
            onConfirm = {
                scope.launch {
                    viewModel.setFirstTimeOnProvidersScreen(false)
                }.invokeOnCompletion {
                    onNeedHelp()
                }
            },
            onDismiss = {
                scope.run {
                    launch {
                        viewModel.setFirstTimeOnProvidersScreen(false)
                    }.invokeOnCompletion {
                        launch {
                            helpTooltipState.show()
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun MissingProvidersLogo() {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.primary.onMediumEmphasis()
        ) {
            Icon(
                painter = painterResource(id = UiCommonR.drawable.provider_logo),
                contentDescription = stringResource(id = LocaleR.string.missing_providers_logo_content_description),
                modifier = Modifier.size(70.dp)
            )

            Text(
                text = "?",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 25.sp
                ),
                modifier = Modifier
                    .padding(bottom = 2.dp)
            )
        }
    }
}

@Preview
@Composable
private fun MissingProvidersLogoPreview() {
    FlixclusiveTheme {
        Surface {
            EmptyDataMessage(
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.Center
            ) {
                MissingProvidersLogo()
            }
        }
    }
}
