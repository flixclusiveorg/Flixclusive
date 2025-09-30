package com.flixclusive.feature.mobile.provider.manage

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.material3.dialog.IconAlertDialog
import com.flixclusive.core.presentation.mobile.components.material3.dialog.TextAlertDialog
import com.flixclusive.core.presentation.mobile.components.provider.ProviderCrashBottomSheet
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.data.provider.util.extensions.isNotUsable
import com.flixclusive.feature.mobile.provider.manage.component.InstalledProviderCard
import com.flixclusive.feature.mobile.provider.manage.component.ProviderManagerTopBar
import com.flixclusive.feature.mobile.provider.manage.reorderable.ReorderableItem
import com.flixclusive.feature.mobile.provider.manage.reorderable.rememberReorderableLazyGridState
import com.flixclusive.model.provider.ProviderMetadata
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

private val FabButtonSize = 56.dp

private fun Context.getHelpGuideTexts() = resources.getStringArray(LocaleR.array.providers_screen_help)

@Destination
@Composable
internal fun ProviderManagerScreen(
    navigator: ProviderManagerScreenNavigator,
    viewModel: ProviderManagerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val providerToggles by viewModel.providerToggles.collectAsStateWithLifecycle()
    val isFirstTimeOnProvidersScreen by viewModel.isFirstTimeOnProvidersScreen.collectAsStateWithLifecycle()

    ProviderManagerScreenContent(
        uiState = uiState,
        providers = {
            if (searchQuery.isNotBlank() && uiState.isSearching) {
                viewModel.providers.filter { it.name.contains(searchQuery, true) }
            } else {
                viewModel.providers
            }
        },
        isFirstTimeOnProvidersScreen = isFirstTimeOnProvidersScreen,
        providerToggles = { providerToggles },
        searchQuery = { searchQuery },
        onQueryChange = viewModel::onQueryChange,
        onMove = viewModel::onMove,
        goBack = navigator::goBack,
        toggleProvider = { id -> viewModel.toggleProvider(id) },
        openProviderSettings = navigator::openProviderSettings,
        onConsumeError = viewModel::onConsumeError,
        openProviderDetails = navigator::openProviderDetails,
        openAddProviderScreen = navigator::openAddProviderScreen,
        uninstallProvider = viewModel::uninstallProvider,
        setFirstTimeOnProvidersScreen = viewModel::setFirstTimeOnProvidersScreen,
        openMarkdownScreen = navigator::openMarkdownScreen,
        onToggleSearchBar = viewModel::onToggleSearchBar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
internal fun ProviderManagerScreenContent(
    uiState: ProviderManageUiState,
    isFirstTimeOnProvidersScreen: Boolean,
    providers: () -> List<ProviderMetadata>,
    providerToggles: () -> List<Boolean>,
    searchQuery: () -> String,
    onQueryChange: (String) -> Unit,
    onMove: suspend (Int, Int) -> Unit,
    goBack: () -> Unit,
    toggleProvider: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    openProviderSettings: (ProviderMetadata) -> Unit,
    openProviderDetails: (ProviderMetadata) -> Unit,
    onConsumeError: () -> Unit,
    openAddProviderScreen: () -> Unit,
    uninstallProvider: (ProviderMetadata) -> Unit,
    setFirstTimeOnProvidersScreen: (Boolean) -> Unit,
    openMarkdownScreen: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var providerToUninstall by rememberSaveable { mutableStateOf<ProviderMetadata?>(null) }

    val view = LocalView.current
    val helpTooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val lazyGridState = rememberLazyGridState()
    val reorderableLazyListState = rememberReorderableLazyGridState(
        lazyGridState = lazyGridState,
        onMove = { from, to ->
            if (!uiState.isSearching) {
                onMove(from.index, to.index)
                ViewCompat.performHapticFeedback(
                    view,
                    HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK,
                )
            }
        },
    )

    val onNeedHelp = {
        val (title, description) = context.getHelpGuideTexts()
        openMarkdownScreen(title, description)
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var isFabExpanded by remember { mutableStateOf(false) }

    val providersAreEmpty by remember {
        derivedStateOf { providers().isEmpty() }
    }

    LaunchedEffect(scrollBehavior.state.heightOffset) {
        delay(800)
        isFabExpanded = scrollBehavior.state.heightOffset < 0f
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ProviderManagerTopBar(
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                tooltipState = helpTooltipState,
                isSearching = uiState.isSearching,
                onToggleSearchBar = onToggleSearchBar,
                onNavigationClick = goBack,
                scrollBehavior = scrollBehavior,
                onNeedHelp = onNeedHelp,
            )
        },
        floatingActionButton = {
            if (!providersAreEmpty) {
                ExtendedFloatingActionButton(
                    onClick = openAddProviderScreen,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    expanded = isFabExpanded,
                    text = {
                        Text(text = stringResource(LocaleR.string.add_providers))
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = UiCommonR.drawable.round_add_24),
                            contentDescription = stringResource(LocaleR.string.add_providers),
                        )
                    },
                )
            }
        },
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(LocalGlobalScaffoldPadding.current),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedContent(
                targetState = providersAreEmpty,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.fillMaxSize(),
            ) { state ->
                if (state) {
                    EmptyDataMessage(
                        description = stringResource(LocaleR.string.empty_providers_list_message),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier =
                                Modifier
                                    .padding(bottom = 12.dp),
                        ) {
                            MissingProvidersLogo()
                            OutlinedButton(
                                onClick = openAddProviderScreen,
                                modifier = Modifier,
                            ) {
                                Text(text = stringResource(LocaleR.string.add_providers))
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        state = lazyGridState,
                        columns = GridCells.Adaptive(getAdaptiveDp(300.dp)),
                        contentPadding = PaddingValues(
                            bottom = FabButtonSize * 1.5f,
                            end = 10.dp,
                            start = 10.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                    ) {
                        itemsIndexed(
                            items = providers(),
                            key = { _, item -> item.id },
                        ) { index, metadata ->
                            ReorderableItem(reorderableLazyListState, metadata.id) { isDragging ->
                                val interactionSource = remember { MutableInteractionSource() }

                                InstalledProviderCard(
                                    providerMetadata = metadata,
                                    interactionSource = interactionSource,
                                    isDraggable = !uiState.isSearching,
                                    openSettings = { openProviderSettings(metadata) },
                                    onClick = { openProviderDetails(metadata) },
                                    uninstallProvider = { providerToUninstall = metadata },
                                    onToggleProvider = { toggleProvider(metadata.id) },
                                    enabledProvider = {
                                        val isDisabled = providerToggles().getOrNull(index)

                                        !metadata.isNotUsable && isDisabled == false
                                    },
                                    isDraggingProvider = { isDragging },
                                    dragModifier =
                                        Modifier.draggableHandle(
                                            onDragStarted = {
                                                ViewCompat.performHapticFeedback(
                                                    view,
                                                    HapticFeedbackConstantsCompat.GESTURE_START,
                                                )
                                            },
                                            onDragStopped = {
                                                ViewCompat.performHapticFeedback(
                                                    view,
                                                    HapticFeedbackConstantsCompat.GESTURE_END,
                                                )
                                            },
                                            interactionSource = interactionSource,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (providerToUninstall != null) {
        val metadata = remember { providerToUninstall!! }

        IconAlertDialog(
            painter = painterResource(id = UiCommonR.drawable.warning),
            contentDescription = stringResource(id = LocaleR.string.warning_content_description),
            description =
                buildAnnotatedString {
                    append(context.getString(LocaleR.string.warning_uninstall_message_first_half))
                    append(" ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(metadata.name)
                    }
                    append("?")
                },
            onConfirm = {
                uninstallProvider(metadata)
                providerToUninstall = null
            },
            onDismiss = { providerToUninstall = null },
        )
    }

    if (isFirstTimeOnProvidersScreen) {
        TextAlertDialog(
            title = stringResource(LocaleR.string.first_time_providers_screen_title),
            message = stringResource(LocaleR.string.first_time_providers_screen_message),
            dismissButtonLabel = stringResource(id = LocaleR.string.skip),
            dismissOnConfirm = false,
            onConfirm = {
                setFirstTimeOnProvidersScreen(false)
                onNeedHelp()
            },
            onDismiss = {
                setFirstTimeOnProvidersScreen(false)
                scope.launch {
                    helpTooltipState.show()
                }
            },
        )
    }

    if (uiState.error != null) {
        ProviderCrashBottomSheet(
            isLoading = false,
            errors = listOf(uiState.error),
            onDismissRequest = onConsumeError,
        )
    }
}

@Composable
private fun MissingProvidersLogo() {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.primary.copy(0.6f),
        ) {
            Icon(
                painter = painterResource(id = UiCommonR.drawable.provider_logo),
                contentDescription = stringResource(id = LocaleR.string.missing_providers_logo_content_description),
                modifier = Modifier.size(70.dp),
            )

            Text(
                text = "?",
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 25.sp,
                    ),
                modifier =
                    Modifier
                        .padding(bottom = 2.dp),
            )
        }
    }
}

@Preview
@Composable
private fun ProviderManagerScreenBasePreview() {
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<ProviderWithThrowable?>(null) }
    var uiState by remember(error) { mutableStateOf(ProviderManageUiState(error = error)) }

    val list = remember {
        mutableStateListOf<ProviderMetadata>().also {
            it.addAll(
                List(20) {
                    DummyDataForPreview.getDummyProviderMetadata(
                        id = it.toString(),
                        name = "Provider #$it",
                    )
                },
            )
        }
    }

    val toggles = remember {
        mutableStateListOf<Boolean>().also {
            it.addAll(List(list.size) { it % 3 == 0 })
        }
    }

    FlixclusiveTheme {
        Surface {
            ProviderManagerScreenContent(
                uiState = uiState,
                isFirstTimeOnProvidersScreen = false,
                providers = {
                    if (query.isNotBlank() && uiState.isSearching) {
                        list.filter { it.name.contains(query, true) }
                    } else {
                        list
                    }
                },
                providerToggles = { toggles },
                searchQuery = { query },
                onQueryChange = { query = it },
                onMove = { from, to ->
                    list.add(to, list.removeAt(from))
                    toggles.add(to, toggles.removeAt(from))
                },
                goBack = {},
                toggleProvider = { id ->
                    val index = list.indexOfFirst { it.id == id }

                    if (index > -1) {
                        if (Random.nextBoolean()) {
                            error = ProviderWithThrowable(
                                provider = list[index],
                                throwable = Throwable("This is a dummy error for $id"),
                            )

                            return@ProviderManagerScreenContent
                        }

                        toggles[index] = !toggles[index]
                    }
                },
                onConsumeError = { error = null },
                openProviderSettings = {},
                openProviderDetails = {},
                openAddProviderScreen = {},
                uninstallProvider = {},
                setFirstTimeOnProvidersScreen = {},
                onToggleSearchBar = { uiState = uiState.copy(isSearching = it) },
                openMarkdownScreen = { _, _ -> },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ProviderManagerScreenCompactLandscapePreview() {
    ProviderManagerScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ProviderManagerScreenMediumPortraitPreview() {
    ProviderManagerScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ProviderManagerScreenMediumLandscapePreview() {
    ProviderManagerScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ProviderManagerScreenExtendedPortraitPreview() {
    ProviderManagerScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ProviderManagerScreenExtendedLandscapePreview() {
    ProviderManagerScreenBasePreview()
}
