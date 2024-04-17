package com.flixclusive.feature.mobile.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.provider.InstalledProviderCard
import com.flixclusive.core.ui.mobile.util.getFeedbackOnLongPress
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.feature.mobile.provider.component.HeaderButtons
import com.flixclusive.feature.mobile.provider.component.ProvidersTopBar
import com.flixclusive.feature.mobile.provider.util.DragAndDropUtils.dragGestureHandler
import com.flixclusive.feature.mobile.provider.util.rememberDragDropListState
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.Status
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.Job
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

interface ProvidersScreenNavigator : GoBackAction {
    fun openProviderSettings(providerData: ProviderData)

    fun openAddRepositoryScreen()
}

@Destination
@Composable
fun ProvidersScreen(
    navigator: ProvidersScreenNavigator
) {
    val viewModel = hiltViewModel<ProvidersScreenViewModel>()
    val providerSettings by viewModel.providerSettings.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val overscrollJob = remember { mutableStateOf<Job?>(null) }
    val dragDropListState = rememberDragDropListState(onMove = viewModel::onMove)
    val listState = dragDropListState.getLazyListState()
    val shouldShowTopBar by listState.isScrollingUp()

    val searchExpanded = rememberSaveable { mutableStateOf(false) }

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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                    HeaderButtons(
                        modifier = Modifier
                            .padding(top = 15.dp, bottom = 10.dp)
                    )
                }

                item {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = LocalContentColor.current.onMediumEmphasis(0.4F)
                    )
                }

                itemsIndexed(filteredProviders ?: viewModel.providerDataList) { index, providerData ->
                    val displacementOffset =
                        if (index + 2 == dragDropListState.getCurrentIndexOfDraggedListItem()) {
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
                        uninstallProvider = { viewModel.uninstallProvider(index) },
                        onToggleProvider = { viewModel.toggleProvider(providerData) }
                    )
                }
            }
        }
    }
}