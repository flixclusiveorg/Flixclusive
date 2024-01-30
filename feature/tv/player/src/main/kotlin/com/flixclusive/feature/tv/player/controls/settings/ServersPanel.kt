package com.flixclusive.feature.tv.player.controls.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.PlayerUiState
import com.flixclusive.core.ui.tv.util.hasPressedRight
import com.flixclusive.feature.tv.player.controls.settings.common.ListContentHolder
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.provider.base.Provider
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun ServersPanel(
    modifier: Modifier = Modifier,
    state: PlayerUiState,
    servers: List<SourceLink>,
    providers: List<Provider>,
    onProviderChange: (String) -> Unit,
    onServerChange: (Int) -> Unit,
    hidePanel: () -> Unit,
) {
    val selectedSourceIndex = remember(state.selectedProvider) {
        providers.indexOfFirst { it.name.equals(state.selectedProvider, true) }
    }

    var isFirstItemFullyFocused by remember { mutableStateOf(true) }

    val blackBackgroundGradient = Brush.horizontalGradient(
        0F to Color.Black,
        0.85F to Color.Black.onMediumEmphasis(),
        1F to Color.Black.onMediumEmphasis(0.4F),
    )

    BackHandler {
        hidePanel()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(blackBackgroundGradient),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.85F)
        ) {
            ListContentHolder(
                modifier = Modifier
                    .weight(1F)
                    .onPreviewKeyEvent {
                        isFirstItemFullyFocused = false
                        false
                    },
                contentDescription = stringResource(id = UtilR.string.providers),
                label = stringResource(id = UtilR.string.providers),
                items = providers,
                selectedIndex = selectedSourceIndex,
                itemState = state.selectedProviderState,
                onItemClick = {
                    onProviderChange(providers[it].name)
                }
            )

            ListContentHolder(
                modifier = Modifier
                    .weight(1F)
                    .onKeyEvent {
                        if (hasPressedRight(it) && isFirstItemFullyFocused) {
                            hidePanel()
                            return@onKeyEvent true
                        } else isFirstItemFullyFocused = true

                        false
                    },
                contentDescription = stringResource(id = UtilR.string.servers),
                label = stringResource(id = UtilR.string.servers),
                items = servers,
                initializeFocus = true,
                selectedIndex = state.selectedSourceLink,
                onItemClick = onServerChange
            )
        }
    }
}

