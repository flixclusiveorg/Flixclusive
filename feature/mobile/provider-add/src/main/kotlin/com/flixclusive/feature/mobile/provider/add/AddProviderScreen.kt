package com.flixclusive.feature.mobile.provider.add

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.material3.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.core.presentation.mobile.components.provider.ProviderCrashBottomSheet
import com.flixclusive.core.presentation.mobile.components.provider.ProviderInstallState
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.feature.mobile.provider.add.component.AddProviderTopBar
import com.flixclusive.feature.mobile.provider.add.component.ErrorScreen
import com.flixclusive.feature.mobile.provider.add.component.ProviderCard
import com.flixclusive.feature.mobile.provider.add.component.ProviderCardPlaceholder
import com.flixclusive.feature.mobile.provider.add.component.RepositoryCrashBottomSheet
import com.flixclusive.feature.mobile.provider.add.filter.AddProviderFilterType
import com.flixclusive.feature.mobile.provider.add.filter.AuthorsFilters
import com.flixclusive.feature.mobile.provider.add.filter.CommonSortFilters
import com.flixclusive.feature.mobile.provider.add.filter.component.AddProviderFilterBottomSheet
import com.flixclusive.model.provider.ProviderMetadata
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.launch
import com.flixclusive.core.strings.R as LocaleR

@Destination<ExternalModuleGraph>(
    navArgs = AddProviderScreenNavArgs::class,
)
@Composable
internal fun AddProviderScreen(
    navigator: NavigatorAddProviderScreen,
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
        selectedProviders = { selectedProviders },
        searchQuery = { searchQuery },
        onToggleSearchBar = viewModel::onToggleSearchBar,
        onRetry = viewModel::initialize,
        onGoBack = navigator::navigateBack,
        consumeProviderExceptions = viewModel::consumeProviderExceptions,
        onToggleInstallState = viewModel::onToggleInstallState,
        onUninstallProvider = viewModel::onUninstall,
        onConfigure = navigator::navigateToProviderSettings,
        onViewProviderDetails = navigator::showProviderDetailsSheet,
        onQueryChange = viewModel::onSearchQueryChange,
        onUpdateFilter = viewModel::onUpdateFilter,
        onRepositoryClick = viewModel::onRepositoryFilterClick,
        filters = { filters },
        installStates = viewModel.providerInstallStates,
        onToggleSelect = viewModel::onToggleSelect,
        onUnselectAll = viewModel::onUnselectAll,
        onInstallSelection = viewModel::onInstallSelection,
        providers = {
            if (uiState.isShowingSearchBar) searchResults else availableProviders
        },
    )
}

@Composable
internal fun AddProviderScreenContent(
    uiState: AddProviderUiState,
    selectedProviders: () -> ImmutableSet<ProviderMetadata>,
    searchQuery: () -> String,
    providers: () -> Async<PersistentList<ProviderItem>>,
    filters: () -> Async<PersistentList<AddProviderFilterType<*>>>,
    installStates: Map<String, ProviderInstallState>,
    onRetry: () -> Unit,
    onGoBack: () -> Unit,
    onInstallSelection: () -> Unit,
    onUnselectAll: () -> Unit,
    consumeProviderExceptions: () -> Unit,
    onToggleInstallState: (ProviderMetadata) -> Unit,
    onUninstallProvider: (ProviderMetadata) -> Unit,
    onConfigure: (ProviderMetadata) -> Unit,
    onViewProviderDetails: (ProviderMetadata) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onToggleSelect: (ProviderMetadata) -> Unit,
    onUpdateFilter: (Int, AddProviderFilterType<*>) -> Unit,
    onRepositoryClick: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = rememberEnterAlwaysScrollBehavior()

    var isFilterSheetOpened by remember { mutableStateOf(false) }
    var isRepositoryCrashOpened by remember { mutableStateOf(false) }
    val selectCount by remember { derivedStateOf { selectedProviders().size } }
    val isLoading by remember { derivedStateOf { providers() is Async.Loading } }

    LaunchedEffect(uiState.repositoryExceptions) {
        if (uiState.repositoryExceptions.isNotEmpty()) {
            isRepositoryCrashOpened = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AddProviderTopBar(
                isSearching = uiState.isShowingSearchBar,
                isLoading = isLoading,
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
    ) { scaffoldPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            AsyncAnimatedContent(
                targetState = providers(),
                modifier = Modifier.fillMaxSize(),
                loadingContent = {
                    LazyColumn(
                        contentPadding = scaffoldPadding,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(10) {
                            ProviderCardPlaceholder(modifier = Modifier.animateItem())
                        }
                    }
                },
                errorContent = {
                    ErrorScreen(onRetry = onRetry)
                },
                content = { getProviders ->
                    val currentProviders = getProviders()
                    if (currentProviders.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyDataMessage(
                                emojiHeader = "📂",
                                title = stringResource(LocaleR.string.this_seems_empty),
                                description = stringResource(LocaleR.string.no_installable_providers),
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = scaffoldPadding,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(currentProviders, key = { it.id + it.name }) { item ->
                                val interactionSource = remember { MutableInteractionSource() }
                                val shape = MaterialTheme.shapes.small

                                ProviderCard(
                                    provider = item.metadata,
                                    installState = { installStates[item.id] ?: ProviderInstallState.NotInstalled },
                                    isSelected = selectedProviders().contains(item.metadata),
                                    onClick = { onToggleInstallState(item.metadata) },
                                    onUninstall = { onUninstallProvider(item.metadata) },
                                    onConfigure = { onConfigure(item.metadata) },
                                    onRepositoryClick = { onRepositoryClick(item.metadata.repositoryUrl) },
                                    shape = shape,
                                    modifier = Modifier
                                        .animateItem()
                                        .clip(shape)
                                        .indication(
                                            interactionSource = interactionSource,
                                            indication = ripple(),
                                        ).pointerInput(Unit) {
                                            detectTapGestures(
                                                onLongPress = { _ ->
                                                    val canBeSelected =
                                                        installStates[item.id] is ProviderInstallState.NotInstalled ||
                                                            installStates[item.id] is ProviderInstallState.Outdated

                                                    if (canBeSelected) {
                                                        onToggleSelect(item.metadata)
                                                    }
                                                },
                                                onTap = { _ ->
                                                    val isSelecting = selectedProviders().isNotEmpty()
                                                    val canBeSelected =
                                                        installStates[item.id] is ProviderInstallState.NotInstalled ||
                                                            installStates[item.id] is ProviderInstallState.Outdated

                                                    if (isSelecting && !canBeSelected) {
                                                        scope.launch {
                                                            val message = resources.getString(
                                                                R.string.error_selection_on_installed_provider,
                                                                item.name
                                                            )

                                                            snackbarHostState.showSnackbar(
                                                                message,
                                                                withDismissAction = true,
                                                            )
                                                        }
                                                        return@detectTapGestures
                                                    }

                                                    if (isSelecting) {
                                                        onToggleSelect(item.metadata)
                                                    } else {
                                                        onViewProviderDetails(item.metadata)
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
                },
            )
        }
    }

    if (isFilterSheetOpened) {
        AddProviderFilterBottomSheet(
            filters = { (filters() as? Async.Success)?.data ?: emptyList() },
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
            isLoading = isLoading,
            errors = uiState.repositoryExceptions,
            onDismissRequest = { isRepositoryCrashOpened = false },
        )
    }
}

@Preview
@Composable
private fun AddProviderScreenBasePreview() {
    val uiState = remember { AddProviderUiState() }
    val sampleProvider = DummyDataForPreview.getProviderMetadata()
    val providers = remember { mutableStateListOf<ProviderItem>() }
    val filters = remember { mutableStateListOf<AddProviderFilterType<*>>() }

    val providersAsync = remember(providers.size) {
        if (providers.isEmpty()) {
            Async.Loading
        } else {
            Async.Success(providers.toPersistentList())
        }
    }
    val filtersAsync = remember(filters.size) {
        if (filters.isEmpty()) {
            Async.Loading
        } else {
            Async.Success(filters.toPersistentList())
        }
    }

    LaunchedEffect(true) {
        providers.addAll(
            List(50) {
                ProviderItem.from(
                    sampleProvider.copy(name = "${sampleProvider.name} #$it"),
                )
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
    }
    val selectedProviders = remember { mutableStateListOf<ProviderMetadata>() }
    var searchQuery by remember { mutableStateOf("") }

    FlixclusiveTheme {
        Surface {
            AddProviderScreenContent(
                uiState = uiState,
                selectedProviders = { selectedProviders.toPersistentSet() },
                searchQuery = { searchQuery },
                onQueryChange = { searchQuery = it },
                onRetry = {},
                onGoBack = {},
                onToggleSearchBar = {},
                onUpdateFilter = { _, _ -> },
                filters = { filtersAsync },
                providers = { providersAsync },
                onInstallSelection = {},
                onToggleInstallState = {},
                onUninstallProvider = {},
                onConfigure = {},
                onViewProviderDetails = {},
                onUnselectAll = { selectedProviders.clear() },
                consumeProviderExceptions = {},
                onRepositoryClick = {},
                installStates = mapOf(),
                onToggleSelect = {
                    if (selectedProviders.contains(it)) {
                        selectedProviders.remove(it)
                    } else {
                        selectedProviders.add(it)
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
