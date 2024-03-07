package com.flixclusive.feature.mobile.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.util.getFeedbackOnLongPress
import com.flixclusive.feature.mobile.provider.component.HeaderButtons
import com.flixclusive.feature.mobile.provider.component.ProviderCard
import com.flixclusive.feature.mobile.provider.util.DragAndDropUtils.dragGestureHandler
import com.flixclusive.feature.mobile.provider.util.rememberDragDropListState
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.Job
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

interface ProvidersScreenNavigator : GoBackAction {
    fun openProviderSettings(pluginName: String)

    fun openAddProviderScreen()
}

@Destination
@Composable
fun ProvidersScreen(
    navigator: ProvidersScreenNavigator
) {
    val viewModel = hiltViewModel<ProvidersScreenViewModel>()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val overscrollJob = remember { mutableStateOf<Job?>(null) }
    val dragDropListState = rememberDragDropListState(onMove = viewModel::onMove)
    val listState = dragDropListState.getLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = Modifier
                .dragGestureHandler(
                    scope = coroutineScope,
                    itemListDragAndDropState = dragDropListState,
                    overscrollJob = overscrollJob,
                    feedbackLongPress = getFeedbackOnLongPress()
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = UtilR.string.providers),
                        style = MaterialTheme.typography.headlineMedium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )

                    IconButton(onClick = { viewModel.isSearching }) {
                        Icon(
                            painter = painterResource(id = UiCommonR.drawable.search_outlined),
                            contentDescription = stringResource(id = UtilR.string.search_for_providers)
                        )
                    }
                }
            }

            item {
                HeaderButtons()
            }

            item {
                Divider(
                    thickness = 1.dp,
                    color = LocalContentColor.current.onMediumEmphasis(0.4F)
                )
            }

            itemsIndexed(viewModel.pluginDataMap.values.toList()) { index, pluginData ->
                val enabled = remember(appSettings) {
                    !appSettings[index].isDisabled
                }

                val displacementOffset =
                    if (index == dragDropListState.getCurrentIndexOfDraggedListItem()) {
                        dragDropListState.elementDisplacement.takeIf { it != 0f }
                    } else null

                ProviderCard(
                    pluginData = pluginData,
                    enabled = enabled,
                    isSearching = viewModel.isSearching,
                    displacementOffset = displacementOffset,
                    openSettings = { navigator.openProviderSettings(pluginData.name) },
                    uninstallProvider = { viewModel.uninstallPlugin(pluginData.name) },
                    onToggleProvider = {
                        viewModel.togglePlugin(index)
                    }
                )
            }
        }
    }
}