package com.flixclusive.feature.mobile.plugin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.TopBarWithBackButton
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.mobile.util.getFeedbackOnLongPress
import com.flixclusive.core.ui.mobile.util.isAtTop
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.feature.mobile.plugin.component.PluginCard
import com.flixclusive.feature.mobile.plugin.util.DragAndDropUtils.dragGestureHandler
import com.flixclusive.feature.mobile.plugin.util.rememberDragDropListState
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.Job
import com.flixclusive.core.util.R as UtilR

@Destination
@Composable
fun PluginsScreen(
    navigator: GoBackAction
) {
    val viewModel = hiltViewModel<PluginScreenViewModel>()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val overscrollJob = remember { mutableStateOf<Job?>(null) }
    val dragDropListState = rememberDragDropListState(onMove = viewModel::onMove)
    val listState = dragDropListState.getLazyListState()
    val shouldShowTopBar by listState.isScrollingUp()
    val listIsAtTop by listState.isAtTop()

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            AnimatedVisibility(
                visible = shouldShowTopBar,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                TopBarWithBackButton(
                    headerTitle = stringResource(id = UtilR.string.providers),
                    onNavigationIconClick = navigator::goBack
                )
            }
        }
    ) { innerPadding ->
        val topPadding by animateDpAsState(
            targetValue = if (listIsAtTop) innerPadding.calculateTopPadding() else 0.dp,
            label = ""
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(top = topPadding)
                .dragGestureHandler(
                    scope = coroutineScope,
                    itemListDragAndDropState = dragDropListState,
                    overscrollJob = overscrollJob,
                    feedbackLongPress = getFeedbackOnLongPress()
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(viewModel.pluginDataMap.values.toList()) { index, pluginData ->
                val enabled = remember(appSettings) {
                    !appSettings[index].isDisabled
                }

                val displacementOffset =
                    if (index == dragDropListState.getCurrentIndexOfDraggedListItem()) {
                        dragDropListState.elementDisplacement.takeIf { it != 0f }
                    } else null

                PluginCard(
                    pluginData = pluginData,
                    enabled = enabled,
                    displacementOffset = displacementOffset,
                    openSettings = {
                        /*
                        * TODO("Navigate to a plugins settings screen, then
                        *  pass the SettingsScreen() of the pluginData as a parameter")
                        * */
                        /*viewModel.plugins[index].SettingsScreen()*/
                    },
                    unloadPlugin = { viewModel.uninstallPlugin(pluginData.name) },
                    onToggleProvider = {
                        viewModel.togglePlugin(index)
                    }
                )
            }
        }
    }
}