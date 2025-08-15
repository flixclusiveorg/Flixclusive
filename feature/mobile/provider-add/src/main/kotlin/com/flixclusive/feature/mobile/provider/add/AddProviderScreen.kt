package com.flixclusive.feature.mobile.provider.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.strings.UiText
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.dialog.IconAlertDialog
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewProviderAction
import com.flixclusive.core.ui.common.util.DummyDataForPreview
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.mobile.component.EmptyDataMessage
import com.flixclusive.core.ui.mobile.component.LoadingScreen
import com.flixclusive.core.ui.mobile.component.PlainTooltipBox
import com.flixclusive.core.ui.mobile.component.provider.ProviderCard
import com.flixclusive.core.ui.mobile.component.provider.ProviderCardDefaults
import com.flixclusive.core.ui.mobile.component.provider.ProviderInstallationStatus
import com.flixclusive.core.ui.mobile.component.topbar.ActionButton
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarWithSearch
import com.flixclusive.core.ui.mobile.component.topbar.DefaultNavigationIcon
import com.flixclusive.core.ui.mobile.component.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.core.ui.mobile.util.ComposeUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.feature.mobile.provider.add.component.ErrorScreen
import com.flixclusive.feature.mobile.provider.add.filter.AddProviderFilterType
import com.flixclusive.feature.mobile.provider.add.filter.AuthorsFilters
import com.flixclusive.feature.mobile.provider.add.filter.CommonSortFilters
import com.flixclusive.feature.mobile.provider.add.filter.component.AddProviderFilterBottomSheet
import com.flixclusive.feature.mobile.provider.add.util.getErrorLog
import com.flixclusive.model.provider.ProviderMetadata
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.collections.immutable.toImmutableList
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

interface AddProviderScreenNavigator :
    ViewProviderAction,
    GoBackAction

@Destination
@Composable
internal fun AddProviderScreen(
    navigator: AddProviderScreenNavigator,
    viewModel: AddProviderViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val availableProviders = viewModel.availableProviders.collectAsStateWithLifecycle()
    val selectedProviders = viewModel.selectedProviders.collectAsStateWithLifecycle()
    val filters = viewModel.filters.collectAsStateWithLifecycle()

    val searchQuery by remember {
        derivedStateOf { uiState.value.searchQuery }
    }

    val isShowingFilterSheet by remember {
        derivedStateOf { uiState.value.isShowingFilterSheet }
    }

    val isSearching by remember {
        derivedStateOf { uiState.value.isShowingSearchBar }
    }

    val isLoading by remember {
        derivedStateOf { uiState.value.isLoading }
    }

    val hasInitializationErrors by remember {
        derivedStateOf { uiState.value.hasInitializationErrors }
    }

    var showErrorDialog by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.initializeError
            .collect { state ->
                if (!showErrorDialog && state) {
                    showErrorDialog = true
                }
            }
    }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.providerLoadErrors
            .collect { errors ->
                if (snackbarHostState.currentSnackbarData == null && errors.isNotEmpty()) {
                    val message =
                        """
                        ${context.getString(LocaleR.string.failed_to_load_providers)}: ${errors.joinToString(",")}
                        """.trimIndent()

                    snackbarHostState.showSnackbar(
                        message = message,
                        withDismissAction = true,
                    )
                }
            }
    }

    if (showErrorDialog) {
        IconAlertDialog(
            painter = painterResource(UiCommonR.drawable.warning_outline),
            contentDescription = null,
            description = context.getErrorLog(uiState.value.failedToInitializeRepositories),
            dismissButtonLabel = null,
            onConfirm = { showErrorDialog = false },
        )
    }

    AddProviderScreen(
        isLoading = isLoading,
        hasInitializationErrors = hasInitializationErrors,
        isSearching = isSearching,
        isShowingFilterSheet = isShowingFilterSheet,
        snackbarHostState = snackbarHostState,
        installationStatusMap = viewModel.providerInstallationStatusMap,
        selectedProviders = { selectedProviders.value },
        searchQuery = { searchQuery },
        onToggleSearchBar = viewModel::onToggleSearchBar,
        onRetry = viewModel::initialize,
        onGoBack = navigator::goBack,
        onToggleInstallation = viewModel::onToggleInstallation,
        onViewProviderDetails = navigator::openProviderDetails,
        onQueryChange = viewModel::onSearchQueryChange,
        onToggleFilterSheet = viewModel::onToggleFilterSheet,
        onUpdateFilter = viewModel::onUpdateFilter,
        filters = { filters.value },
        providers = { availableProviders.value },
        onToggleSelect = viewModel::onToggleSelect,
        onUnselectAll = viewModel::onUnselectAll,
        onInstallSelection = viewModel::onInstallSelection,
    )
}

@Composable
internal fun AddProviderScreen(
    isLoading: Boolean,
    hasInitializationErrors: Boolean,
    isSearching: Boolean,
    snackbarHostState: SnackbarHostState,
    isShowingFilterSheet: Boolean,
    installationStatusMap: Map<String, ProviderInstallationStatus>,
    selectedProviders: () -> List<ProviderMetadata>,
    searchQuery: () -> String,
    providers: () -> List<ProviderMetadata>,
    filters: () -> List<AddProviderFilterType<*>>,
    onRetry: () -> Unit,
    onGoBack: () -> Unit,
    onInstallSelection: () -> Unit,
    onUnselectAll: () -> Unit,
    onToggleInstallation: (ProviderMetadata) -> Unit,
    onViewProviderDetails: (ProviderMetadata) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onToggleFilterSheet: (Boolean) -> Unit,
    onToggleSelect: (ProviderMetadata) -> Unit,
    onUpdateFilter: (Int, AddProviderFilterType<*>) -> Unit,
) {
    val scrollBehavior = rememberEnterAlwaysScrollBehavior()
    val selectedColor = MaterialTheme.colorScheme.tertiary

    val selectCount by remember {
        derivedStateOf { selectedProviders().size }
    }

    Scaffold(
        modifier =
            Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = getAdaptiveDp(DefaultScreenPaddingHorizontal, 2.dp))
                .fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AddProviderTopBar(
                isSearching = isSearching,
                isLoading = isLoading,
                selectCount = { selectCount },
                scrollBehavior = scrollBehavior,
                searchQuery = searchQuery,
                onToggleSearchBar = onToggleSearchBar,
                onQueryChange = onQueryChange,
                onNavigate = onGoBack,
                onShowFilterSheet = { onToggleFilterSheet(true) },
                onInstallSelection = onInstallSelection,
                onUnselectAll = onUnselectAll,
            )
        },
    ) {
        val padding by remember {
            derivedStateOf { it }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isLoading) {
                LoadingScreen()
            } else if (providers().isEmpty() && hasInitializationErrors) {
                ErrorScreen(onRetry = onRetry)
            } else if (providers().isEmpty()) {
                EmptyDataMessage(
                    emojiHeader = "📂",
                    title = stringResource(LocaleR.string.this_seems_empty),
                    description = stringResource(LocaleR.string.no_installable_providers),
                )
            } else {
                LazyColumn(
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    items(providers(), key = { it.id + it.name }) {
                        val interactionSource = remember { MutableInteractionSource() }
                        val installationStatus = installationStatusMap[it.id] ?: ProviderInstallationStatus.NotInstalled

                        ProviderCard(
                            providerMetadata = it,
                            onClick = { onToggleInstallation(it) },
                            status = installationStatus,
                            modifier =
                                Modifier
                                    .animateItem()
                                    .indication(
                                        interactionSource = interactionSource,
                                        indication = ripple(),
                                    ).border(
                                        BorderStroke(
                                            width = Dp.Hairline,
                                            color =
                                                if (selectedProviders().contains(it)) {
                                                    selectedColor
                                                } else {
                                                    Color.Transparent
                                                },
                                        ),
                                        shape = ProviderCardDefaults.DefaultShape,
                                    ).pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { _ -> onToggleSelect(it) },
                                            onTap = { _ ->
                                                val isSelecting = selectedProviders().isNotEmpty()
                                                if (isSelecting) {
                                                    onToggleSelect(it)
                                                } else {
                                                    onViewProviderDetails(it)
                                                }
                                            },
                                            onPress = { offset ->
                                                val press = PressInteraction.Press(offset)
                                                interactionSource.emit(press)
                                                val released = tryAwaitRelease()
                                                interactionSource.emit(
                                                    if (released) {
                                                        PressInteraction.Release(press)
                                                    } else {
                                                        PressInteraction.Cancel(press)
                                                    },
                                                )
                                            },
                                        )
                                    },
                        )
                    }
                }
            }
        }
    }

    if (isShowingFilterSheet) {
        AddProviderFilterBottomSheet(
            filters = filters,
            onDismissRequest = { onToggleFilterSheet(false) },
            onUpdateFilter = onUpdateFilter,
        )
    }
}

@Composable
private fun AddProviderTopBar(
    isSearching: Boolean,
    isLoading: Boolean,
    selectCount: () -> Int,
    searchQuery: () -> String,
    onNavigate: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onInstallSelection: () -> Unit,
    onShowFilterSheet: () -> Unit,
    onUnselectAll: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val title =
        if (selectCount() > 0) {
            stringResource(LocaleR.string.count_selection_format, selectCount())
        } else {
            stringResource(LocaleR.string.add_providers)
        }

    CommonTopBarWithSearch(
        modifier = modifier,
        isSearching = isSearching,
        title = title,
        onNavigate = onNavigate,
        navigationIcon = {
            if (selectCount() > 0) {
                PlainTooltipBox(description = stringResource(LocaleR.string.cancel)) {
                    ActionButton(onClick = onUnselectAll) {
                        AdaptiveIcon(
                            painter = painterResource(UiCommonR.drawable.round_close_24),
                            contentDescription = stringResource(LocaleR.string.cancel),
                        )
                    }
                }
            } else {
                DefaultNavigationIcon(
                    onClick = {
                        if (isSearching) {
                            onToggleSearchBar(false)
                        } else {
                            onNavigate()
                        }
                    },
                )
            }
        },
        searchQuery = searchQuery,
        onToggleSearchBar = onToggleSearchBar,
        onQueryChange = onQueryChange,
        scrollBehavior = scrollBehavior,
        extraActions = {
            if (selectCount() > 0 && !isLoading) {
                PlainTooltipBox(description = stringResource(LocaleR.string.install_all)) {
                    ActionButton(onClick = onInstallSelection) {
                        AdaptiveIcon(
                            painter = painterResource(UiCommonR.drawable.download),
                            contentDescription = stringResource(LocaleR.string.install_all),
                            dp = 24.dp,
                        )
                    }
                }
            } else if (!isLoading) {
                PlainTooltipBox(description = stringResource(LocaleR.string.filter_button)) {
                    ActionButton(onClick = onShowFilterSheet) {
                        AdaptiveIcon(
                            painter = painterResource(UiCommonR.drawable.filter_list),
                            contentDescription = stringResource(LocaleR.string.filter_button),
                            dp = 24.dp,
                        )
                    }
                }
            }
        },
    )
}

@Preview
@Composable
private fun AddProviderScreenBasePreview() {
    var state by remember { mutableStateOf(AddProviderUiState()) }
    val filters = remember { mutableStateListOf<AddProviderFilterType<*>>() }
    val providers = remember { mutableStateListOf<ProviderMetadata>() }

    val sampleProvider = DummyDataForPreview.getDummyProviderMetadata()
    LaunchedEffect(true) {
        providers.addAll(
            List(50) { sampleProvider.copy(name = "${sampleProvider.name} #$it") },
        )
        filters.addAll(
            listOf<AddProviderFilterType<*>>(
                CommonSortFilters(
                    title = UiText.StringValue("Sort by option"),
                    selectedValue = AddProviderFilterType.Sort.SortSelection(0),
                ),
                AuthorsFilters(
                    title = UiText.StringValue("Sort by item"),
                    options = List(20) { "Item $it" }.toImmutableList(),
                    selectedValue = setOf(),
                ),
            ),
        )
    }

    val isShowingFilterSheet by remember {
        derivedStateOf { state.isShowingFilterSheet }
    }

    val isSearching by remember {
        derivedStateOf { state.isShowingSearchBar }
    }

    val selectedProviders by remember {
        derivedStateOf { state.selectedProviders }
    }

    val searchQuery by remember {
        derivedStateOf { state.searchQuery }
    }

    FlixclusiveTheme {
        Surface {
            AddProviderScreen(
                isLoading = false,
                hasInitializationErrors = false,
                snackbarHostState = remember { SnackbarHostState() },
                isShowingFilterSheet = isShowingFilterSheet,
                isSearching = isSearching,
                installationStatusMap = mapOf(),
                selectedProviders = { selectedProviders },
                searchQuery = { searchQuery },
                onQueryChange = { state = state.copy(searchQuery = it) },
                onRetry = {},
                onGoBack = {},
                onToggleSearchBar = { state = state.copy(isShowingSearchBar = it) },
                onToggleFilterSheet = { state = state.copy(isShowingFilterSheet = it) },
                onUpdateFilter = { i, filter -> filters[i] = filter },
                filters = { filters },
                providers = { providers },
                onInstallSelection = {},
                onToggleInstallation = {},
                onViewProviderDetails = {},
                onUnselectAll = { state = state.copy(selectedProviders = emptyList()) },
                onToggleSelect = {
                    state =
                        with(state) {
                            val newList =
                                if (selectedProviders.contains(it)) {
                                    val newList = selectedProviders.toMutableList()
                                    newList.remove(it)
                                    newList.toList()
                                } else {
                                    selectedProviders + it
                                }

                            copy(selectedProviders = newList)
                        }
                },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun AddProviderScreenCompactLandscapePreview() {
    AddProviderScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun AddProviderScreenMediumPortraitPreview() {
    AddProviderScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun AddProviderScreenMediumLandscapePreview() {
    AddProviderScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun AddProviderScreenExtendedPortraitPreview() {
    AddProviderScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun AddProviderScreenExtendedLandscapePreview() {
    AddProviderScreenBasePreview()
}
