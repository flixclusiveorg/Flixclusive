package com.flixclusive.feature.mobile.provider

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.ProviderUninstallNoticeDialog
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.navigation.ProviderTestNavigator
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.ui.mobile.component.provider.InstalledProviderCard
import com.flixclusive.core.ui.mobile.util.getFeedbackOnLongPress
import com.flixclusive.core.ui.mobile.util.isAtTop
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.feature.mobile.provider.component.CustomButton
import com.flixclusive.feature.mobile.provider.component.ProfileHandlerButtons
import com.flixclusive.feature.mobile.provider.component.ProvidersTopBar
import com.flixclusive.feature.mobile.provider.util.DragAndDropUtils.dragGestureHandler
import com.flixclusive.feature.mobile.provider.util.rememberDragDropListState
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.Status
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.Job
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

interface ProvidersScreenNavigator : GoBackAction, ProviderTestNavigator {
    fun openProviderSettings(providerData: ProviderData)
    fun openProviderInfo(providerData: ProviderData)
    fun openAddRepositoryScreen()
}

@Destination
@Composable
fun ProvidersScreen(
    navigator: ProvidersScreenNavigator
) {
    val context = LocalContext.current

    val viewModel = hiltViewModel<ProvidersScreenViewModel>()
    val providerSettings by viewModel.providerSettings.collectAsStateWithLifecycle()
    val searchExpanded = rememberSaveable { mutableStateOf(false) }
    var indexOfProviderToUninstall by rememberSaveable { mutableStateOf<Int?>(null) }

    val coroutineScope = rememberCoroutineScope()
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


    val filteredProviders by remember {
        derivedStateOf {
            when (viewModel.searchQuery.isNotEmpty()) {
                true -> viewModel.providerDataList.filter {
                    it.name.contains(viewModel.searchQuery, true)
                }
                false -> null
            }
        }
    }

    val featureComingSoonCallback = {
        context.showToast(context.getString(UtilR.string.coming_soon_feature))
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ProvidersTopBar(
                isVisible = shouldShowTopBar,
                searchQuery = viewModel.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                searchExpanded = searchExpanded
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = navigator::openAddRepositoryScreen,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                expanded = !shouldShowTopBar,
                text = {
                    Text(text = stringResource(UtilR.string.add_provider))
                },
                icon = {
                    Icon(
                        painter = painterResource(id = UiCommonR.drawable.round_add_24),
                        contentDescription = stringResource(UtilR.string.add_provider)
                    )
                }
            )
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
                targetState = viewModel.providerDataList.isEmpty(),
                label = ""
            ) { state ->
                if (state) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = UtilR.string.empty_providers_list_message),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .dragGestureHandler(
                                scope = coroutineScope,
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
                                                providers = viewModel.providerDataList
                                                    .toCollection(ArrayList())
                                            )
                                        },
                                        iconId = UiCommonR.drawable.test,
                                        label = stringResource(id = UtilR.string.test_providers),
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

                        itemsIndexed(items = filteredProviders ?: viewModel.providerDataList) { index, providerData ->
                            val displacementOffset =
                                // +1 since there's a header
                                if (index + 1 == dragDropListState.getCurrentIndexOfDraggedListItem()) {
                                    dragDropListState.elementDisplacement.takeIf { it != 0f }
                                } else null

                            val isEnabled = providerData.status != Status.Maintenance
                                && providerData.status != Status.Down
                                && (providerSettings.getOrNull(index)?.isDisabled?.not() ?: true)

                            InstalledProviderCard(
                                providerData = providerData,
                                enabled = isEnabled,
                                isDraggable = !searchExpanded.value,
                                displacementOffset = displacementOffset,
                                openSettings = { navigator.openProviderSettings(providerData) },
                                onClick = { navigator.openProviderInfo(providerData) },
                                uninstallProvider = { indexOfProviderToUninstall = index },
                                onToggleProvider = { viewModel.toggleProvider(providerData) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (indexOfProviderToUninstall != null) {
        val providerData = remember { (filteredProviders ?: viewModel.providerDataList)[indexOfProviderToUninstall!!] }
        ProviderUninstallNoticeDialog(
            providerData = providerData,
            onConfirm = {
                viewModel.uninstallProvider(indexOfProviderToUninstall!!)
                indexOfProviderToUninstall = null
            },
            onDismiss = { indexOfProviderToUninstall = null }
        )
    }
}