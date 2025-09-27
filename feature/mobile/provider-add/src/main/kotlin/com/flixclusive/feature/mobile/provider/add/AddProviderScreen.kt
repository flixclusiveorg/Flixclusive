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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.ProviderInstallationStatus
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.LoadingScreen
import com.flixclusive.core.presentation.mobile.components.material3.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.core.presentation.mobile.components.provider.ProviderCard
import com.flixclusive.core.presentation.mobile.components.provider.ProviderCardDefaults
import com.flixclusive.core.presentation.mobile.components.provider.ProviderCrashBottomSheet
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.feature.mobile.provider.add.component.AddProviderTopBar
import com.flixclusive.feature.mobile.provider.add.component.ErrorScreen
import com.flixclusive.feature.mobile.provider.add.component.RepositoryCrashBottomSheet
import com.flixclusive.feature.mobile.provider.add.filter.AddProviderFilterType
import com.flixclusive.feature.mobile.provider.add.filter.AuthorsFilters
import com.flixclusive.feature.mobile.provider.add.filter.CommonSortFilters
import com.flixclusive.feature.mobile.provider.add.filter.component.AddProviderFilterBottomSheet
import com.flixclusive.model.provider.ProviderMetadata
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import com.flixclusive.core.strings.R as LocaleR

@Destination(
    navArgsDelegate = AddProviderScreenNavArgs::class,
)
@Composable
internal fun AddProviderScreen(
    navigator: AddProviderScreenNavigator,
    viewModel: AddProviderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableProviders by viewModel.availableProviders.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedProviders by viewModel.selected.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    AddProviderScreenContent(
        uiState = uiState,
        installationStatusMap = viewModel.providerInstallationStatusMap,
        selectedProviders = { selectedProviders },
        searchQuery = { searchQuery },
        onToggleSearchBar = viewModel::onToggleSearchBar,
        onRetry = viewModel::initialize,
        onGoBack = navigator::goBack,
        consumeProviderExceptions = viewModel::consumeProviderExceptions,
        onToggleInstallation = viewModel::onToggleInstallation,
        onViewProviderDetails = navigator::openProviderDetails,
        onQueryChange = viewModel::onSearchQueryChange,
        onUpdateFilter = viewModel::onUpdateFilter,
        filters = { filters },
        onToggleSelect = viewModel::onToggleSelect,
        onUnselectAll = viewModel::onUnselectAll,
        onInstallSelection = viewModel::onInstallSelection,
        providers = {
            if (uiState.isShowingSearchBar) {
                searchResults
            } else {
                availableProviders
            }
        },
    )
}

@Composable
internal fun AddProviderScreenContent(
    uiState: AddProviderUiState,
    installationStatusMap: Map<String, ProviderInstallationStatus>,
    selectedProviders: () -> ImmutableSet<ProviderMetadata>,
    searchQuery: () -> String,
    providers: () -> ImmutableList<SearchableProvider>,
    filters: () -> List<AddProviderFilterType<*>>,
    onRetry: () -> Unit,
    onGoBack: () -> Unit,
    onInstallSelection: () -> Unit,
    onUnselectAll: () -> Unit,
    consumeProviderExceptions: () -> Unit,
    onToggleInstallation: (ProviderMetadata) -> Unit,
    onViewProviderDetails: (ProviderMetadata) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onToggleSelect: (ProviderMetadata) -> Unit,
    onUpdateFilter: (Int, AddProviderFilterType<*>) -> Unit,
) {
    val scrollBehavior = rememberEnterAlwaysScrollBehavior()
    val selectedColor = MaterialTheme.colorScheme.tertiary

    var isFilterSheetOpened by remember { mutableStateOf(false) }
    var isRepositoryCrashOpened by remember { mutableStateOf(false) }
    val selectCount by remember { derivedStateOf { selectedProviders().size } }

    LaunchedEffect(uiState.repositoryExceptions.isNotEmpty(), uiState.isLoading) {
        if (uiState.repositoryExceptions.isNotEmpty() && uiState.isLoading) {
            isRepositoryCrashOpened = true
        }
    }

    Scaffold(
        topBar = {
            AddProviderTopBar(
                isSearching = uiState.isShowingSearchBar,
                isLoading = uiState.isLoading,
                selectCount = selectCount,
                scrollBehavior = scrollBehavior,
                searchQuery = searchQuery,
                onToggleSearchBar = onToggleSearchBar,
                onQueryChange = onQueryChange,
                onNavigate = onGoBack,
                onShowFilterSheet = { isFilterSheetOpened = true },
                onInstallSelection = onInstallSelection,
                onUnselectAll = onUnselectAll,
            )
        },
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(horizontal = getAdaptiveDp(DefaultScreenPaddingHorizontal, 2.dp))
            .fillMaxSize(),
    ) {
        val padding by remember { derivedStateOf { it } }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (uiState.isLoading) {
                LoadingScreen()
            } else if (providers().isEmpty() && uiState.repositoryExceptions.isNotEmpty()) {
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
                            providerMetadata = it.metadata,
                            onClick = { onToggleInstallation(it.metadata) },
                            status = installationStatus,
                            modifier =
                                Modifier
                                    .animateItem()
                                    .indication(
                                        interactionSource = interactionSource,
                                        indication = ripple(),
                                    )
                                    .border(
                                        BorderStroke(
                                            width = Dp.Hairline,
                                            color =
                                                if (selectedProviders().contains(it.metadata)) {
                                                    selectedColor
                                                } else {
                                                    Color.Transparent
                                                },
                                        ),
                                        shape = ProviderCardDefaults.DefaultShape,
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { _ -> onToggleSelect(it.metadata) },
                                            onTap = { _ ->
                                                val isSelecting = selectedProviders().isNotEmpty()
                                                if (isSelecting) {
                                                    onToggleSelect(it.metadata)
                                                } else {
                                                    onViewProviderDetails(it.metadata)
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

    if (isFilterSheetOpened) {
        AddProviderFilterBottomSheet(
            filters = filters,
            onDismissRequest = { isFilterSheetOpened = false },
            onUpdateFilter = onUpdateFilter,
        )
    }

    if (uiState.providerExceptions.isNotEmpty()) {
        ProviderCrashBottomSheet(
            isLoading = uiState.isInstallingProviders,
            errors = uiState.providerExceptions,
            onDismissRequest = consumeProviderExceptions,
        )
    }

    if (isRepositoryCrashOpened) {
        RepositoryCrashBottomSheet(
            isLoading = uiState.isLoading,
            errors = uiState.repositoryExceptions,
            onDismissRequest = { isRepositoryCrashOpened = false },
        )
    }
}

@Preview
@Composable
private fun AddProviderScreenBasePreview() {
    var state by remember { mutableStateOf(AddProviderUiState(isLoading = true)) }
    val filters = remember { mutableStateListOf<AddProviderFilterType<*>>() }
    val providers = remember { mutableStateListOf<SearchableProvider>() }

    val sampleProvider = DummyDataForPreview.getDummyProviderMetadata()
    LaunchedEffect(true) {
        providers.addAll(
            List(50) {
                SearchableProvider.from(sampleProvider.copy(name = "${sampleProvider.name} #$it"))
            },
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

        state = state.copy(isLoading = false)
    }
    val selectedProviders = remember { mutableStateListOf<ProviderMetadata>() }
    var searchQuery by remember { mutableStateOf("") }

    FlixclusiveTheme {
        Surface {
            AddProviderScreenContent(
                uiState = state,
                installationStatusMap = mapOf(),
                selectedProviders = { selectedProviders.toPersistentSet() },
                searchQuery = { searchQuery },
                onQueryChange = { searchQuery = it },
                onRetry = {},
                onGoBack = {},
                onToggleSearchBar = { state = state.copy(isShowingSearchBar = it) },
                onUpdateFilter = { i, filter -> filters[i] = filter },
                filters = { filters },
                providers = { providers.toPersistentList() },
                onInstallSelection = {},
                onToggleInstallation = {},
                onViewProviderDetails = {},
                onUnselectAll = { selectedProviders.clear() },
                consumeProviderExceptions = { state = state.copy(providerExceptions = emptyList()) },
                onToggleSelect = {
                    if (selectedProviders.contains(it)) {
                        selectedProviders.remove(it)
                    } else {
                        selectedProviders + it
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
